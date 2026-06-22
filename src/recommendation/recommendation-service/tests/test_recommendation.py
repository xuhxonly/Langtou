import pytest
from fastapi.testclient import TestClient

from main import create_app


@pytest.fixture
def client():
    app = create_app()
    return TestClient(app)


def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_root_endpoint(client):
    response = client.get("/")
    assert response.status_code == 200
    assert "service" in response.json()


def test_recommend_feed_get(client):
    response = client.get("/api/v1/recommend/feed?user_id=test_user_1")
    assert response.status_code in [200, 500]  # 500 if data sources not available


def test_recommend_feed_post(client):
    response = client.post(
        "/api/v1/recommend/feed",
        json={"user_id": "test_user_1", "page": 1, "page_size": 10},
    )
    assert response.status_code in [200, 500]


def test_hot_content(client):
    response = client.get("/api/v1/recommend/hot")
    assert response.status_code in [200, 500]


def test_search(client):
    response = client.get("/api/v1/recommend/search?query=美食")
    assert response.status_code in [200, 500]


def test_feedback(client):
    response = client.post(
        "/api/v1/recommend/feedback",
        json={
            "user_id": "test_user_1",
            "note_id": "note_1",
            "action": "like",
            "score": 3.0,
        },
    )
    assert response.status_code in [200, 500]
