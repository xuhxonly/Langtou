import json
from typing import Any, Dict, List, Tuple

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.data import redis_client, es_client

# 中文停用词列表（简化版，实际可使用更完整的停用词表）
CHINESE_STOP_WORDS = {
    "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也",
    "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那",
    "个", "之", "与", "及", "等", "或", "但", "而", "因为", "所以", "如果", "虽然", "但是",
    "可以", "这个", "那个", "这些", "那些", "什么", "怎么", "为什么", "如何", "哪里", "谁",
    "时候", "现在", "今天", "明天", "昨天", "这里", "那里", "这样", "那样", "非常", "已经",
    "正在", "曾经", "还是", "或者", "还是", "并且", "而且", "不过", "只是", "只有", "只要",
    "无论", "不管", "尽管", "即使", "哪怕", "就算", "既", "又", "一边", "一方面", "另",
    "另外", "此外", "其次", "然后", "接着", "最后", "最终", "总之", "综上所述", "例如",
    "比如", "像", "如同", "似的", "似乎", "好像", "一样", "一般", "通常", "常常", "经常",
    "往往", "一直", "始终", "永远", "暂时", "临时", "突然", "忽然", "猛然", "渐渐", "逐渐",
    "逐步", "慢慢", "快", "慢", "早", "晚", "多", "少", "大", "小", "高", "低", "长", "短",
    "远", "近", "深", "浅", "厚", "薄", "重", "轻", "硬", "软", "冷", "热", "干", "湿",
    "新", "旧", "老", "年轻", "美丽", "漂亮", "好看", "丑", "坏", "好", "对", "错", "真",
    "假", "虚", "实", "空", "满", "整", "零", "半", "双", "单", "每", "各", "某", "另",
    "别", "其", "他", "她", "它", "它们", "她们", "他们", "我们", "咱们", "大家", "大伙",
    "人家", "别人", "旁人", "自己", "本人", "本", "此", "这", "那", "这里", "那里", "这边",
    "那边", "这会儿", "那会儿", "这么", "那么", "这样", "那样", "几", "多少", "一些", "一点",
    "许多", "很多", "不少", "大量", "少量", "部分", "全部", "全体", "整体", "整个", "完全",
    "彻底", "根本", "基本", "大概", "大约", "约", "差不多", "几乎", "简直", "根本", "决",
    "绝对", "完全", "都", "全", "总", "统统", "通通", "一概", "一律", "总是", "老是", "一直",
    "一向", "从来", "始终", "毕竟", "究竟", "到底", "终归", "终究", "结果", "后果", "成果",
    "然后", "而后", "之后", "后来", "以后", "今后", "往后", "向后", "向前", "向上", "向下",
    "向内", "向外", "在家", "出国", "回来", "回去", "过来", "过去", "上来", "上去", "下来",
    "下去", "进来", "进去", "出来", "出去", "起来", "过", "完", "到", "给", "把", "被",
    "让", "叫", "使", "令", "请", "求", "劝", "催", "逼", "强迫", "命令", "允许", "同意",
    "赞成", "支持", "反对", "拒绝", "接受", "收到", "受到", "得到", "获得", "失去", "丢失",
    "找", "寻", "搜", "查", "看", "见", "望", "盯", "瞧", "瞥", "瞪", "眯", "睁", "闭",
    "听", "闻", "嗅", "尝", "吃", "喝", "咬", "嚼", "吞", "咽", "吐", "吹", "吸", "呼",
    "喊", "叫", "嚷", "吼", "啼", "鸣", "唱", "说", "讲", "谈", "聊", "唠", "叨", "念",
    "读", "写", "画", "描", "绘", "涂", "抹", "擦", "洗", "刷", "扫", "拖", "抬", "搬",
    "扛", "背", "抱", "搂", "搀", "扶", "拉", "推", "拖", "拽", "扯", "拔", "插", "按",
    "压", "挤", "捏", "拧", "扭", "掐", "抓", "挠", "挖", "掏", "掘", "砍", "劈", "剁",
    "切", "割", "剪", "缝", "补", "钉", "粘", "贴", "系", "绑", "捆", "扎", "绕", "缠",
    "卷", "铺", "盖", "遮", "挡", "掩", "护", "守", "卫", "保", "护", "维", "修", "整",
    "理", "治", "管", "办", "处", "置", "安", "排", "布", "置", "设", "计", "划", "策",
    "谋", "算", "计", "估", "量", "测", "量", "称", "量", "数", "算", "加", "减", "乘",
    "除", "等于", "大于", "小于", "超过", "不足", "够", "足", "满", "空", "缺", "剩", "余",
    "留", "存", "放", "摆", "挂", "吊", "悬", "架", "支", "撑", "顶", "垫", "衬", "填",
    "塞", "堵", "漏", "渗", "透", "穿", "过", "越", "跨", "跳", "跃", "飞", "跑", "走",
    "行", "爬", "滚", "翻", "转", "旋", "绕", "回", "返", "归", "来", "去", "往", "向",
    "朝", "对", "面", "背", "靠", "依", "倚", "躺", "卧", "趴", "伏", "跪", "蹲", "坐",
    "站", "立", "挺", "直", "弯", "曲", "斜", "歪", "倒", "倾", "覆", "翻", "滚", "落",
    "掉", "降", "升", "提", "举", "抬", "扛", "背", "负", "载", "装", "卸", "拆", "卸",
    "装", "配", "套", "组", "合", "并", "分", "离", "开", "关", "闭", "合", "聚", "散",
    "集", "结", "团", "队", "群", "众", "伙", "帮", "派", "系", "列", "行", "排", "列",
    "队", "组", "班", "级", "届", "期", "代", "辈", "代", "世", "纪", "年", "月", "日",
    "时", "分", "秒", "刻", "钟", "点", "段", "节", "章", "篇", "页", "面", "行", "字",
    "词", "句", "段", "节", "章", "篇", "部", "集", "卷", "册", "本", "份", "件", "张",
    "片", "块", "条", "根", "支", "枝", "杆", "棵", "株", "朵", "瓣", "粒", "颗", "滴",
    "点", "团", "堆", "批", "群", "帮", "伙", "队", "组", "班", "排", "连", "营", "团",
    "师", "军", "将", "帅", "王", "皇", "帝", "后", "妃", "嫔", "妾", "奴", "仆", "佣",
    "工", "农", "商", "学", "兵", "官", "吏", "员", "役", "差", "徒", "弟", "兄", "姐",
    "妹", "哥", "弟", "叔", "伯", "姑", "姨", "舅", "婶", "嫂", "婿", "媳", "夫", "妻",
    "父", "母", "子", "女", "儿", "孙", "祖", "宗", "族", "家", "户", "门", "姓", "名",
    "字", "号", "称", "呼", "叫", "唤", "喊", "嚷", "吼", "啼", "鸣", "唱", "歌", "曲",
    "调", "腔", "韵", "律", "声", "音", "响", "亮", "噪", "静", "寂", "寞", "孤", "独",
    "单", "双", "对", "副", "套", "身", "件", "条", "块", "片", "张", "页", "面", "方",
    "圆", "长", "短", "高", "低", "深", "浅", "厚", "薄", "宽", "窄", "粗", "细", "大",
    "小", "多", "少", "轻", "重", "快", "慢", "急", "缓", "早", "晚", "先", "后", "前",
    "后", "左", "右", "东", "西", "南", "北", "中", "内", "外", "里", "表", "上", "下",
    "头", "尾", "始", "终", "起", "止", "开", "关", "生", "死", "活", "灭", "存", "亡",
    "兴", "衰", "盛", "败", "成", "败", "胜", "负", "赢", "输", "得", "失", "利", "弊",
    "益", "害", "好", "坏", "优", "劣", "强", "弱", "刚", "柔", "软", "硬", "冷", "热",
    "温", "凉", "干", "湿", "潮", "燥", "晴", "阴", "雨", "雪", "风", "霜", "雾", "露",
    "雷", "电", "云", "霞", "虹", "日", "月", "星", "辰", "天", "地", "山", "川", "河",
    "湖", "海", "洋", "江", "溪", "涧", "泉", "池", "潭", "沟", "渠", "井", "泉", "源",
    "流", "波", "浪", "涛", "潮", "汛", "洪", "涝", "旱", "灾", "难", "祸", "福", "吉",
    "凶", "祥", "瑞", "兆", "征", "候", "象", "形", "状", "态", "势", "况", "景", "象",
    "色", "彩", "光", "影", "声", "响", "味", "香", "臭", "腥", "膻", "臊", "酸", "甜",
    "苦", "辣", "咸", "淡", "涩", "麻", "鲜", "美", "香", "臭", "腥", "膻", "臊", "酸",
    "甜", "苦", "辣", "咸", "淡", "涩", "麻", "鲜", "美", "香", "臭", "腥", "膻", "臊",
}


class ContentSimilarityModel:
    """
    Content-based similarity model using TF-IDF on note text.
    """

    def __init__(self, model_path: str = None):
        self.model_path = model_path or "./saved_models/content_model.json"
        self.vectorizer = TfidfVectorizer(max_features=5000, stop_words=list(CHINESE_STOP_WORDS))
        self.tfidf_matrix = None
        self.note_ids = []

    def build(self, notes: List[Dict[str, Any]]):
        """
        Build content similarity model from notes.
        """
        self.note_ids = [n.get("id", n.get("note_id", "")) for n in notes]
        texts = []

        for note in notes:
            # Combine title, content, and tags
            tags = note.get("tags", [])
            if isinstance(tags, list):
                tags_str = " ".join(str(t) for t in tags)
            elif isinstance(tags, str):
                tags_str = tags
            else:
                tags_str = ""

            text_parts = [
                note.get("title", ""),
                note.get("content", ""),
                tags_str,
            ]
            texts.append(" ".join(text_parts))

        # Fit TF-IDF
        self.tfidf_matrix = self.vectorizer.fit_transform(texts)

        return self

    def get_similar_items(self, note_id: str, top_k: int = 20) -> List[Tuple[str, float]]:
        """Get top-k content-similar items."""
        if note_id not in self.note_ids or self.tfidf_matrix is None:
            return []

        idx = self.note_ids.index(note_id)
        note_vector = self.tfidf_matrix[idx]

        # Compute similarity with all notes
        similarities = cosine_similarity(note_vector, self.tfidf_matrix).flatten()

        # Get top-k (excluding self)
        top_indices = np.argsort(similarities)[::-1][1:top_k + 1]

        results = []
        for i in top_indices:
            if similarities[i] > 0:
                results.append((self.note_ids[i], float(similarities[i])))

        return results

    def compute_note_embedding(self, note_id: str) -> np.ndarray:
        """Get TF-IDF embedding for a note."""
        if note_id not in self.note_ids or self.tfidf_matrix is None:
            return np.zeros(self.tfidf_matrix.shape[1] if self.tfidf_matrix is not None else 100)

        idx = self.note_ids.index(note_id)
        return self.tfidf_matrix[idx].toarray().flatten()

    def save(self, path: str = None):
        """Save model to disk as JSON (safe serialization)."""
        path = path or self.model_path
        import os
        os.makedirs(os.path.dirname(path), exist_ok=True)

        # Serialize TF-IDF matrix to list
        tfidf_dense = self.tfidf_matrix.toarray().tolist() if self.tfidf_matrix is not None else []

        # Serialize vectorizer vocabulary
        vocab = self.vectorizer.vocabulary_ if self.vectorizer else {}

        data = {
            "tfidf_matrix": tfidf_dense,
            "note_ids": self.note_ids,
            "vocabulary": vocab,
            "idf": self.vectorizer.idf_.tolist() if hasattr(self.vectorizer, "idf_") and self.vectorizer.idf_ is not None else [],
        }

        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False)

    def load(self, path: str = None):
        """Load model from disk."""
        path = path or self.model_path
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            tfidf_dense = np.array(data["tfidf_matrix"])
            self.tfidf_matrix = tfidf_dense if tfidf_dense.size > 0 else None
            self.note_ids = data["note_ids"]

            # Reconstruct vectorizer
            if data.get("vocabulary") and data.get("idf"):
                self.vectorizer = TfidfVectorizer(max_features=5000, stop_words=list(CHINESE_STOP_WORDS))
                self.vectorizer.vocabulary_ = {k: int(v) for k, v in data["vocabulary"].items()}
                self.vectorizer.idf_ = np.array(data["idf"])

            return True
        except Exception as e:
            print(f"Failed to load content model: {e}")
            return False

    def update_redis(self):
        """Store content similarities in Redis for fast recall."""
        if self.tfidf_matrix is None:
            return

        for i, note_id in enumerate(self.note_ids):
            similarities = cosine_similarity(
                self.tfidf_matrix[i], self.tfidf_matrix
            ).flatten()
            top_indices = np.argsort(similarities)[::-1][1:21]

            sim_dict = {}
            for idx in top_indices:
                if similarities[idx] > 0:
                    sim_dict[self.note_ids[idx]] = float(similarities[idx])

            if sim_dict:
                redis_client.zadd(f"content_sim:{note_id}", sim_dict, expire=86400 * 7)


content_model = ContentSimilarityModel()
