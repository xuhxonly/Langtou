import math
from datetime import datetime
from typing import Any, Dict, List, Optional

import numpy as np


class ContextFeatureExtractor:
    """Extract context-related features for ranking."""

    def __init__(self):
        pass

    def extract(self, user_id: str, note_id: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """Extract all context features."""
        features = {}

        # Time context
        features.update(self._extract_time_features(context))

        # Device context
        features.update(self._extract_device_features(context))

        # Location context
        features.update(self._extract_location_features(context))

        # Cross features
        features.update(self._extract_cross_features(user_id, note_id, context))

        return features

    def _extract_time_features(self, context: Dict[str, Any]) -> Dict[str, float]:
        """Extract time-related context features."""
        now = datetime.now()

        # Hour of day (0-23)
        hour = now.hour

        # Day of week (0-6, Monday=0)
        weekday = now.weekday()

        # Is weekend
        is_weekend = 1 if weekday >= 5 else 0

        # Time period encoding
        # 0: early morning (0-6), 1: morning (6-12), 2: afternoon (12-18), 3: evening (18-24)
        if hour < 6:
            time_period = 0
        elif hour < 12:
            time_period = 1
        elif hour < 18:
            time_period = 2
        else:
            time_period = 3

        # Is work time
        is_work_time = 1 if (weekday < 5 and 9 <= hour < 18) else 0

        # Is leisure time
        is_leisure_time = 1 if (is_weekend or hour >= 18 or hour < 9) else 0

        return {
            "ctx_hour": hour,
            "ctx_weekday": weekday,
            "ctx_is_weekend": is_weekend,
            "ctx_time_period": time_period,
            "ctx_is_work_time": is_work_time,
            "ctx_is_leisure_time": is_leisure_time,
            "ctx_hour_sin": math.sin(2 * math.pi * hour / 24),
            "ctx_hour_cos": math.cos(2 * math.pi * hour / 24),
        }

    def _extract_device_features(self, context: Dict[str, Any]) -> Dict[str, int]:
        """Extract device-related features."""
        device_type = context.get("device_type", "unknown")
        platform = context.get("platform", "unknown")
        app_version = context.get("app_version", "")

        device_map = {
            "mobile": 1,
            "tablet": 2,
            "pc": 3,
            "unknown": 0,
        }

        platform_map = {
            "ios": 1,
            "android": 2,
            "web": 3,
            "unknown": 0,
        }

        # Parse version to number
        version_num = 0
        if app_version:
            try:
                parts = app_version.split(".")
                version_num = int(parts[0]) * 10000 + int(parts[1]) * 100 + int(parts[2])
            except (ValueError, IndexError):
                version_num = 0

        return {
            "ctx_device_type": device_map.get(device_type, 0),
            "ctx_platform": platform_map.get(platform, 0),
            "ctx_app_version": version_num,
            "ctx_is_mobile": 1 if device_type == "mobile" else 0,
        }

    def _extract_location_features(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """Extract location-related features."""
        city = context.get("city", "")
        country = context.get("country", "")

        # Tier city encoding (simplified)
        tier1_cities = {"北京", "上海", "广州", "深圳"}
        tier2_cities = {"杭州", "南京", "成都", "武汉", "西安", "重庆", "天津", "苏州"}

        if city in tier1_cities:
            city_tier = 1
        elif city in tier2_cities:
            city_tier = 2
        elif city:
            city_tier = 3
        else:
            city_tier = 0

        return {
            "ctx_city_tier": city_tier,
            "ctx_has_location": 1 if city else 0,
        }

    def _extract_cross_features(self, user_id: str, note_id: str, context: Dict[str, Any]) -> Dict[str, float]:
        """Extract cross features between user, item, and context."""
        # Session depth (how many items viewed in this session)
        session_depth = context.get("session_depth", 0)

        # Position in feed
        position = context.get("position", 0)

        # Previous interaction type
        prev_interaction = context.get("prev_interaction", "none")
        interaction_map = {
            "none": 0,
            "click": 1,
            "like": 2,
            "share": 3,
            "comment": 4,
            "collect": 5,
        }

        return {
            "ctx_session_depth": session_depth,
            "ctx_position": position,
            "ctx_position_inverse": 1.0 / (position + 1),
            "ctx_prev_interaction": interaction_map.get(prev_interaction, 0),
        }


context_feature_extractor = ContextFeatureExtractor()
