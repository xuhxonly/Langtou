import uuid
from typing import Any, Dict, List

from fastapi import APIRouter, HTTPException, Query

from app.api.models import (
    FeedbackRequest,
    FeedbackResponse,
    HealthResponse,
    HotRequest,
    HotResponse,
    RecommendRequest,
    RecommendResponse,
    SearchRequest,
    SearchResponse,
)
from app.data import es_client, mysql_client, redis_client
from app.engine import recall_engine, rank_engine, reranker
from config import get_settings

router = APIRouter(prefix="/api/v1/recommend")


def _get_note_details(note_ids: List[str]) -> List[Dict[str, Any]]:
    """Fetch note details for a list of note IDs."""
    notes = []
    for nid in note_ids:
        # Try Redis first
        note = redis_client.hgetall(f"note:{nid}")
        if not note:
            note = mysql_client.get_note(nid)
        if not note:
            note = es_client.get_note(nid)
        if note:
            notes.append(note)
    return notes


@router.get("/feed", response_model=RecommendResponse)
async def get_feed(
    user_id: str = Query(..., description="User ID"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    ab_test_group: str = Query(None, description="A/B test group (control/treatment)"),
):
    """
    Get personalized feed for a user.
    Supports A/B test group parameter to use different ranking strategies.
    """
    trace_id = str(uuid.uuid4())
    settings = get_settings()

    try:
        # Step 1: Recall
        candidates = recall_engine.recall(user_id, context={"page": page})

        # Step 2: Rank (with A/B test group)
        ranked = rank_engine.rank(
            user_id,
            candidates,
            context={"page": page, "position": 0},
            ab_test_group=ab_test_group,
        )

        # Step 3: Re-rank
        final_items = reranker.rerank(user_id, ranked, context={"page": page})

        # Record shown items
        reranker.record_shown(user_id, [item_id for item_id, _ in final_items])

        # Fetch details
        note_ids = [item_id for item_id, _ in final_items]
        notes = _get_note_details(note_ids)

        # Add recommendation score to notes
        score_map = {item_id: score for item_id, score in final_items}
        for note in notes:
            note["rec_score"] = score_map.get(note.get("id", note.get("note_id", "")), 0.0)

        return RecommendResponse(
            user_id=user_id,
            items=notes,
            total=len(notes),
            page=page,
            page_size=page_size,
            trace_id=trace_id,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Recommendation failed: {str(e)}")


@router.post("/feed", response_model=RecommendResponse)
async def post_feed(request: RecommendRequest):
    """
    Get personalized feed with context (POST version).
    """
    trace_id = str(uuid.uuid4())

    try:
        context = request.context or {}
        context["page"] = request.page

        # Step 1: Recall
        candidates = recall_engine.recall(request.user_id, context=context)

        # Step 2: Rank
        ranked = rank_engine.rank(request.user_id, candidates, context=context)

        # Step 3: Re-rank
        final_items = reranker.rerank(request.user_id, ranked, context=context)

        # Record shown items
        reranker.record_shown(request.user_id, [item_id for item_id, _ in final_items])

        # Fetch details
        note_ids = [item_id for item_id, _ in final_items]
        notes = _get_note_details(note_ids)

        score_map = {item_id: score for item_id, score in final_items}
        for note in notes:
            note["rec_score"] = score_map.get(note.get("id", note.get("note_id", "")), 0.0)

        return RecommendResponse(
            user_id=request.user_id,
            items=notes,
            total=len(notes),
            page=request.page,
            page_size=request.page_size,
            trace_id=trace_id,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Recommendation failed: {str(e)}")


@router.get("/hot", response_model=HotResponse)
async def get_hot(
    category: str = Query(None, description="Category filter"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
):
    """
    Get hot/trending content.
    """
    try:
        # Get hot notes from MySQL
        hot_notes = mysql_client.get_hot_notes(limit=page_size * page)

        if category:
            hot_notes = [n for n in hot_notes if n.get("category") == category]

        # Pagination
        start = (page - 1) * page_size
        end = start + page_size
        paginated = hot_notes[start:end]

        return HotResponse(
            items=paginated,
            total=len(hot_notes),
            page=page,
            page_size=page_size,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Hot content failed: {str(e)}")


@router.post("/hot", response_model=HotResponse)
async def post_hot(request: HotRequest):
    """Get hot/trending content (POST version)."""
    return await get_hot(
        category=request.category,
        page=request.page,
        page_size=request.page_size,
    )


@router.post("/feedback", response_model=FeedbackResponse)
async def post_feedback(request: FeedbackRequest):
    """
    Record user feedback for a note.
    """
    try:
        # Map action to interaction type
        action_map = {
            "click": ("click", 1.0),
            "like": ("like", 3.0),
            "share": ("share", 5.0),
            "comment": ("comment", 4.0),
            "collect": ("collect", 4.0),
            "dismiss": ("dismiss", -1.0),
        }

        interaction_type, default_score = action_map.get(request.action, ("click", 1.0))
        score = request.score if request.score != 1.0 else default_score

        # Save to MySQL
        mysql_client.add_interaction(
            request.user_id,
            request.note_id,
            interaction_type,
            score,
        )

        # Update Redis user history
        history_key = f"user_history:{request.user_id}"
        redis_client.lpush(history_key, [request.note_id], expire=86400 * 7)

        # Update user tags if it's a positive interaction
        if score > 0:
            note = mysql_client.get_note(request.note_id)
            if note and note.get("tags"):
                if isinstance(note["tags"], str):
                    tags = [t.strip() for t in note["tags"].split(",") if t.strip()]
                elif isinstance(note["tags"], list):
                    tags = [str(t).strip() for t in note["tags"] if str(t).strip()]
                else:
                    tags = []
                tag_key = f"user_tags:{request.user_id}"
                for tag in tags:
                    current = redis_client.hget(tag_key, tag)
                    current_score = float(current) if current else 0.0
                    redis_client.hset(tag_key, tag, current_score + score, expire=86400 * 30)

        return FeedbackResponse(success=True, message="Feedback recorded successfully")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Feedback failed: {str(e)}")


@router.get("/search", response_model=SearchResponse)
async def get_search(
    query: str = Query(..., min_length=1, max_length=200),
    user_id: str = Query(None),
    tags: List[str] = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
):
    """
    Search notes with optional personalization.
    """
    try:
        # Search in ES
        results = es_client.search(query, tags=tags, size=page_size * page)

        # If user_id provided, re-rank based on user preferences
        if user_id and results:
            # Get user tags
            tag_key = f"user_tags:{user_id}"
            user_tags = redis_client.hgetall(tag_key)

            if user_tags:
                # Boost items matching user interests
                boosted = []
                for note in results:
                    note_tags = set(note.get("tags", []))
                    user_tag_set = set(user_tags.keys())
                    match_count = len(note_tags & user_tag_set)
                    boost = 1.0 + 0.1 * match_count
                    note["search_score"] = note.get("_score", 0) * boost
                    boosted.append(note)
                boosted.sort(key=lambda x: x["search_score"], reverse=True)
                results = boosted

        # Pagination
        start = (page - 1) * page_size
        end = start + page_size
        paginated = results[start:end]

        return SearchResponse(
            query=query,
            items=paginated,
            total=len(results),
            page=page,
            page_size=page_size,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")


@router.post("/search", response_model=SearchResponse)
async def post_search(request: SearchRequest):
    """Search notes (POST version)."""
    return await get_search(
        query=request.query,
        user_id=request.user_id,
        tags=request.tags,
        page=request.page,
        page_size=request.page_size,
    )


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(status="healthy")
