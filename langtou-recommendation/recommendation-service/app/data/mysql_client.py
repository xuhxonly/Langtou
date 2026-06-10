from typing import Any, Dict, List, Optional

import pymysql
from pymysql.cursors import DictCursor

from config import get_settings


class MySQLClient:
    """MySQL client for persistent data storage."""

    def __init__(self):
        self.settings = get_settings()
        self.connection = None

    def _connect(self):
        if self.connection is None or not self.connection.open:
            self.connection = pymysql.connect(
                host=self.settings.MYSQL_HOST,
                port=self.settings.MYSQL_PORT,
                user=self.settings.MYSQL_USER,
                password=self.settings.MYSQL_PASSWORD,
                database=self.settings.MYSQL_DB,
                charset="utf8mb4",
                cursorclass=DictCursor,
            )
        return self.connection

    def execute(self, sql: str, params: Optional[tuple] = None) -> int:
        conn = self._connect()
        with conn.cursor() as cursor:
            affected = cursor.execute(sql, params)
            conn.commit()
            return affected

    def fetchone(self, sql: str, params: Optional[tuple] = None) -> Optional[Dict[str, Any]]:
        conn = self._connect()
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()

    def fetchall(self, sql: str, params: Optional[tuple] = None) -> List[Dict[str, Any]]:
        conn = self._connect()
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()

    def close(self):
        if self.connection and self.connection.open:
            self.connection.close()
            self.connection = None

    # ---- ORM-like helpers for Langtou domain ----

    def get_user(self, user_id: str) -> Optional[Dict[str, Any]]:
        sql = "SELECT * FROM users WHERE user_id = %s"
        return self.fetchone(sql, (user_id,))

    def get_note(self, note_id: str) -> Optional[Dict[str, Any]]:
        sql = "SELECT * FROM notes WHERE note_id = %s"
        return self.fetchone(sql, (note_id,))

    def get_user_interactions(self, user_id: str, interaction_type: Optional[str] = None) -> List[Dict[str, Any]]:
        if interaction_type:
            sql = "SELECT * FROM user_interactions WHERE user_id = %s AND interaction_type = %s ORDER BY create_time DESC"
            return self.fetchall(sql, (user_id, interaction_type))
        sql = "SELECT * FROM user_interactions WHERE user_id = %s ORDER BY create_time DESC"
        return self.fetchall(sql, (user_id,))

    def get_notes_by_author(self, author_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        sql = "SELECT * FROM notes WHERE author_id = %s ORDER BY create_time DESC LIMIT %s"
        return self.fetchall(sql, (author_id, limit))

    def add_interaction(self, user_id: str, note_id: str, interaction_type: str, score: float = 1.0) -> bool:
        sql = """
            INSERT INTO user_interactions (user_id, note_id, interaction_type, score, create_time)
            VALUES (%s, %s, %s, %s, NOW())
            ON DUPLICATE KEY UPDATE score = VALUES(score), create_time = VALUES(create_time)
        """
        try:
            self.execute(sql, (user_id, note_id, interaction_type, score))
            return True
        except Exception as e:
            print(f"MySQL add_interaction error: {e}")
            return False

    def get_hot_notes(self, limit: int = 100) -> List[Dict[str, Any]]:
        sql = """
            SELECT n.*,
                   (n.likes * 1.0 + n.comments * 2.0 + n.shares * 3.0) / 
                   (TIMESTAMPDIFF(HOUR, n.create_time, NOW()) + 2.0) AS hot_score
            FROM notes n
            WHERE n.create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)
            ORDER BY hot_score DESC
            LIMIT %s
        """
        return self.fetchall(sql, (limit,))

    def get_user_followees(self, user_id: str) -> List[str]:
        sql = "SELECT followee_id FROM user_relations WHERE follower_id = %s"
        rows = self.fetchall(sql, (user_id,))
        return [r["followee_id"] for r in rows]


mysql_client = MySQLClient()
