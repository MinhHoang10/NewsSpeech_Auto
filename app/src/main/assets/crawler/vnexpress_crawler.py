# vnexpress_crawler.py
import re
import time
import random
import feedparser
from datetime import datetime
from urllib.parse import urljoin
from bs4 import BeautifulSoup  # <-- THÊM ĐỂ LOẠI HTML

# ------------------------------------------------------------------
# Cấu hình
# ------------------------------------------------------------------
HEADERS = {
    'User-Agent': (
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
        'AppleWebKit/537.36 (KHTML, like Gecko) '
        'Chrome/141.0.0.0 Safari/537.36'
    ),
    'Accept': 'application/rss+xml, text/xml;q=0.9, */*;q=0.8',
    'Accept-Language': 'vi-VN,vi;q=0.9',
    'Connection': 'keep-alive',
}

# ------------------------------------------------------------------
# Làm sạch nội dung: loại bỏ HTML + prompt
# ------------------------------------------------------------------
def extract_text_from_html(html: str) -> str:
    """Lấy text sạch từ HTML (loại thẻ <a>, <p>, v.v.)"""
    if not html:
        return ""
    soup = BeautifulSoup(html, 'html.parser')
    # Loại bỏ các thẻ không cần: script, style, a (nếu muốn loại hoàn toàn)
    for tag in soup(['script', 'style', 'a', 'img']):
        tag.decompose()
    return soup.get_text(separator=' ', strip=True)

def clean_content(text: str) -> str:
    if not text:
        return ""
    # Loại bỏ prompt, URL, ký tự thừa
    patterns = [
        r'Hãy\s*đăng\s*nhập.*',
        r'tài\s*khoản\s*để\s*gửi',
        r'tạo\s*tài\s*khoản',
        r'bình\s*luận.*',
        r'©.*?\d{4}',
        r'Xem\s*thêm.*',
        r'https?://[^\s]+',
    ]
    for p in patterns:
        text = re.sub(p, '', text, flags=re.I | re.DOTALL)
    text = re.sub(r'\s+', ' ', text.strip())
    return text[:500] + ("..." if len(text) > 500 else "")

# ------------------------------------------------------------------
# Crawl bằng RSS (chỉ dùng RSS – ổn định, sạch)
# ------------------------------------------------------------------
def crawl_vnexpress(category: str = 'thoi-su', limit: int = 5):
    """
    Crawl VnExpress bằng RSS:
    - Loại bỏ hoàn toàn thẻ HTML trong content
    - Title, content, link, id, category chính xác
    """
    rss_map = {
        'thoi-su': 'thoi-su',
        'oto-xe-may': 'oto-xe-may',
        'kinh-doanh': 'kinh-doanh',
        'the-gioi': 'the-gioi',
        'giao-duc': 'giao-duc',
        'suc-khoe': 'suc-khoe',
        'doi-song': 'doi-song',
        'du-lich': 'du-lich',
    }

    rss_slug = rss_map.get(category.lower().replace(' ', '-'), 'thoi-su')
    rss_url = f"https://vnexpress.net/rss/{rss_slug}.rss"

    print(f"[VnExpress] Đang crawl RSS: {rss_url} (limit={limit})")

    try:
        feed = feedparser.parse(rss_url)
        if not feed.entries:
            print("[RSS] Không có bài viết.")
            return []
    except Exception as e:
        print(f"[RSS] Lỗi parse: {e}")
        return []

    seen_links = set()
    result = []

    for entry in feed.entries[:limit * 2]:
        link = entry.link.strip()
        if link in seen_links:
            continue
        seen_links.add(link)

        # Title sạch
        title = entry.title.strip()
        if 'href' in title.lower() or 'http' in title:
            continue

        # === LẤY CONTENT SẠCH TỪ HTML ===
        raw_html = entry.get('summary', '') or entry.get('description', '') or ''
        plain_text = extract_text_from_html(raw_html)  # Loại thẻ <a>, <p>, v.v.
        content = clean_content(plain_text)

        if len(content) < 30:
            content = "Xem chi tiết tại VnExpress."

        # ID từ URL
        match = re.search(r'-(\d+)(?:\.html)?$', link)
        article_id = match.group(1) if match else str(hash(link))[-8:]

        # Timestamp chuẩn
        pub_date = entry.get('published_parsed') or entry.get('updated_parsed')
        if pub_date:
            timestamp = datetime(*pub_date[:6]).isoformat()
        else:
            timestamp = datetime.now().isoformat()

        # Category chuẩn
        actual_category = rss_slug.replace('-', ' ').title()
        if actual_category == 'Oto Xe May':
            actual_category = 'Ô tô - Xe máy'

        result.append({
            "id": article_id,
            "title": title,
            "content": content,
            "link": link,
            "timestamp": timestamp,
            "source": "VnExpress",
            "category": actual_category
        })

        if len(result) >= limit:
            break

    print(f"[VnExpress] Hoàn thành: {len(result)} bài")
    return result

# ------------------------------------------------------------------
# TEST
# ------------------------------------------------------------------
if __name__ == "__main__":
    import json

    # Test
    news = crawl_vnexpress('oto-xe-may', limit=2)
    print(json.dumps(news, ensure_ascii=False, indent=2))