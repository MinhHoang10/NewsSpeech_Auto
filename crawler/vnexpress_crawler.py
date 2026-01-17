# vnexpress_crawler.py
import re
import requests # Cần thêm thư viện này để request vào link chi tiết
import feedparser
from datetime import datetime
from bs4 import BeautifulSoup

# ------------------------------------------------------------------
# Cấu hình
# ------------------------------------------------------------------
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
    'Connection': 'keep-alive',
}

def get_full_article_content(url):
    """
    Truy cập vào link bài báo để lấy toàn bộ nội dung
    """
    try:
        response = requests.get(url, headers=HEADERS, timeout=10)
        if response.status_code != 200:
            return None
        
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # Cấu trúc VnExpress: Nội dung nằm trong class 'fck_detail'
        content_block = soup.find('article', class_='fck_detail')
        
        # Nếu không tìm thấy (do cấu trúc bài video hoặc podcast khác biệt)
        if not content_block:
            # Thử tìm div chung
            content_block = soup.find('div', class_='fck_detail')

        if content_block:
            # Lấy tất cả các thẻ p có class Normal (đoạn văn chuẩn của VnEx)
            paragraphs = content_block.find_all('p', class_='Normal')
            
            # Gộp lại thành một đoạn văn bản lớn, ngăn cách bằng xuống dòng
            full_text = '\n\n'.join([p.get_text(strip=True) for p in paragraphs])
            return full_text
        
        return None
    except Exception as e:
        print(f"[Detail] Lỗi lấy nội dung chi tiết {url}: {e}")
        return None

def extract_image_from_summary(html_summary):
    """Lấy ảnh từ summary RSS (vì vào chi tiết đôi khi khó lấy ảnh đại diện hơn)"""
    if not html_summary: return None
    soup = BeautifulSoup(html_summary, 'html.parser')
    img = soup.find('img')
    return img['src'] if img else None

def crawl_vnexpress(category: str = 'thoi-su', limit: int = 50):
    rss_map = {
        'thoi-su': 'thoi-su', 'the-gioi': 'the-gioi', 'kinh-doanh': 'kinh-doanh',
        'bat-dong-san': 'bat-dong-san', 'giai-tri': 'giai-tri', 'the-thao': 'the-thao',
        'phap-luat': 'phap-luat', 'giao-duc': 'giao-duc', 'suc-khoe': 'suc-khoe',
        'doi-song': 'doi-song', 'du-lich': 'du-lich', 'khoa-hoc': 'khoa-hoc',
        'so-hoa': 'so-hoa', 'oto-xe-may': 'oto-xe-may'
    }

    slug = rss_map.get(category.lower().replace(' ', '-'), 'thoi-su')
    rss_url = f"https://vnexpress.net/rss/{slug}.rss"
    print(f"[VnExpress] Bắt đầu crawl '{slug}'...")

    try:
        feed = feedparser.parse(rss_url)
    except Exception as e:
        print(f"[RSS] Lỗi kết nối: {e}")
        return []

    result = []
    seen_links = set()

    for entry in feed.entries:
        if len(result) >= limit: break
        
        link = entry.link.strip()
        if link in seen_links or 'video' in link: continue
        seen_links.add(link)

        # 1. Lấy dữ liệu cơ bản từ RSS
        title = entry.title.strip()
        pub_date = entry.get('published_parsed')
        timestamp = datetime(*pub_date[:6]).isoformat() if pub_date else datetime.now().isoformat()
        
        # Lấy ảnh thumbnail từ RSS (nhanh hơn vào chi tiết)
        img_url = extract_image_from_summary(entry.get('summary', ''))

        # 2. VÀO CHI TIẾT ĐỂ LẤY FULL TEXT
        # Lấy description từ RSS làm dự phòng
        summary_text = BeautifulSoup(entry.get('summary', ''), 'html.parser').get_text(separator=' ', strip=True)
        
        # Gọi hàm lấy full text
        full_content = get_full_article_content(link)
        
        # Nếu không lấy được full text thì dùng tạm summary
        final_content = full_content if full_content and len(full_content) > 100 else summary_text

        # ID
        match = re.search(r'-(\d+)(?:\.html)?$', link)
        article_id = match.group(1) if match else str(hash(link))[-8:]

        result.append({
            "id": article_id,
            "title": title,
            "content": final_content, # <-- Đây là nội dung đầy đủ
            "image": img_url,
            "link": link,
            "timestamp": timestamp,
            "source": "VnExpress",
            "category": slug.replace('-', ' ').title()
        })
        print(f"   + [VnEx] Đã lấy: {title[:30]}... ({len(final_content)} chars)")

    return result

if __name__ == "__main__":
    # Test thử 
    data = crawl_vnexpress('thoi-su', limit=3)
    # In ra độ dài nội dung để kiểm tra
    print(f"Bài 1 dài: {len(data[0]['content'])} ký tự")