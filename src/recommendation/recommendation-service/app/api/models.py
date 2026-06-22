from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class RecommendRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    page: int = Field(1, ge=1, description="Page number")
    page_size: int = Field(20, ge=1, le=100, description="Items per page")
    context: Optional[Dict[str, Any]] = Field(default_factory=dict, description="Context information")


class RecommendResponse(BaseModel):
    user_id: str
    items: List[Dict[str, Any]]
    total: int
    page: int
    page_size: int
    trace_id: Optional[str] = None


class HotRequest(BaseModel):
    category: Optional[str] = Field(None, description="Category filter")
    page: int = Field(1, ge=1)
    page_size: int = Field(20, ge=1, le=100)


class HotResponse(BaseModel):
    items: List[Dict[str, Any]]
    total: int
    page: int
    page_size: int


class FeedbackRequest(BaseModel):
    user_id: str = Field(..., description="User ID")
    note_id: str = Field(..., description="Note ID")
    action: str = Field(..., description="Action type: click, like, share, comment, collect, dismiss")
    score: float = Field(1.0, ge=0, le=10, description="Feedback score")
    context: Optional[Dict[str, Any]] = Field(default_factory=dict)


class FeedbackResponse(BaseModel):
    success: bool
    message: str


class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=200, description="Search query")
    user_id: Optional[str] = Field(None, description="User ID for personalization")
    tags: Optional[List[str]] = Field(None, description="Tag filters")
    page: int = Field(1, ge=1)
    page_size: int = Field(20, ge=1, le=100)


class SearchResponse(BaseModel):
    query: str
    items: List[Dict[str, Any]]
    total: int
    page: int
    page_size: int


class HealthResponse(BaseModel):
    status: str
    version: str = "1.0.0"
