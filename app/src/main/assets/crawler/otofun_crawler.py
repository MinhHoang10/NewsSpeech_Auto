# otofun_crawler.py
import json
import time
import re
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup  # Vẫn dùng để parse sau khi Selenium lấy HTML

def setup_driver(headless=True):
    """Setup Chrome driver với options chống detect bot."""
    options = Options()
    if headless:
        options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    options.add_argument('--disable-blink-features=AutomationControlled')
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    options.add_experimental_option('useAutomationExtension', False)
    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')

    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)
    driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
    return driver

def crawl_otofun(limit=3, headless=True):
    driver = setup_driver(headless)
    url = "https://www.otofun.net/forums/oto-xe-may.2/"

    news_list = []
    try:
        driver.get(url)
        time.sleep(3)  # Đợi JS load

        # Lấy HTML sau khi render
        soup = BeautifulSoup(driver.page_source, 'html.parser')

        # Tìm threads: ưu tiên structItem-title, fallback js-threadListItem hoặc a[href*='/threads/']
        threads = (
                soup.find_all('div', class_='structItem-title') or
                soup.find_all('a', href=re.compile(r'/threads/'))[:limit * 2] or
                soup.find_all('h3', class_=lambda x: x and 'title' in x.lower())[:limit * 2]
        )

        threads = threads[:limit * 2]  # Lấy dư để lọc

        for item in threads:
            if len(news_list) >= limit:
                break

            # Tìm link/title
            a_tag = item.find('a') if item.name != 'a' else item
            if not a_tag or not a_tag.get('href'):
                continue

            title = a_tag.get_text(strip=True)
            link = a_tag['href']
            if link.startswith('/'):
                link = 'https://www.otofun.net' + link
            elif not link.startswith('http'):
                continue

            # Extract ID an toàn từ URL
            match = re.search(r'/threads/[^/]+-(\d+)\.html?$', link)
            article_id = match.group(1) if match else str(hash(link))[-8:]

            # Lấy nội dung chi tiết (dùng Selenium để tránh block)
            content = ""
            try:
                driver.get(link)
                time.sleep(2)
                detail_soup = BeautifulSoup(driver.page_source, 'html.parser')

                # Tìm first post: bbWrapper chuẩn XenForo, fallback
                first_post = (
                        detail_soup.find('div', class_='bbWrapper') or
                        detail_soup.find('article', class_=lambda x: x and 'message-body' in x) or
                        detail_soup.find('div', string=re.compile(r'post:', re.I))
                )
                if first_post:
                    content = first_post.get_text(strip=True)[:300] + "..."
                else:
                    content = "Xem chi tiết tại Otofun."
            except Exception as e:
                content = f"Lỗi tải nội dung: {str(e)[:50]}"

            news_list.append({
                "id": article_id,
                "title": title,
                "content": content,
                "link": link,
                "timestamp": datetime.now().isoformat(),
                "source": "Otofun",
                "category": "oto-xe-may"  # Thêm category từ forum
            })

            time.sleep(1 + (hash(title) % 3))  # Random delay 1-4s

    except Exception as e:
        print(f"Lỗi crawl Otofun: {e}")
        return []
    finally:
        driver.quit()

    return news_list


# === TEST ===
if __name__ == "__main__":
    data = crawl_otofun(limit=3, headless=False)  # headless=False để debug
    print(json.dumps(data, ensure_ascii=False, indent=2))