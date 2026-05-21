-- ==========================================
-- 1. 테이블 생성 (DDL)
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(68) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS question (
                                        id BIGSERIAL PRIMARY KEY,
                                        category VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    target_keywords JSONB
    );
CREATE INDEX IF NOT EXISTS idx_category ON question(category);

CREATE TABLE IF NOT EXISTS interview_session (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    category VARCHAR(50) NOT NULL,
    avg_score INT,
    overall_feedback TEXT,
    avg_duration INT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS answer_log (
                                          id BIGSERIAL PRIMARY KEY,
                                          question_id BIGINT NOT NULL REFERENCES question(id),
    user_answer TEXT NOT NULL,
    ai_feedback TEXT NOT NULL,
    score INT,
    session_id BIGINT NOT NULL REFERENCES interview_session(id),
    missing_keywords JSONB,
    duration INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS review_state (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL REFERENCES users(id),
    question_id BIGINT NOT NULL REFERENCES question(id),
    repetition_count INT NOT NULL,
    easiness_factor DOUBLE PRECISION NOT NULL,
    current_interval INT NOT NULL,
    next_review_date DATE NOT NULL,
    last_reviewed_at DATE NOT NULL,
    UNIQUE(user_id, question_id)
    );

-- ==========================================
-- 2. 초기 데이터 삽입 (DML)
-- ==========================================

-- 테스트 유저 1명 삽입 (비밀번호: test1234!)
INSERT INTO users (email, password, nickname, created_at) VALUES
    ('test@example.com', '$2a$10$EYyaykoEbC5QWhBOdSjJ9uLjAcRxkpRtfLW58EYGunFMm8vLjvBWy', '테스터', NOW());

-- 질문 데이터 전수 삽입 (40개 전체 복원)
INSERT INTO question (category, question_text, target_keywords) VALUES
                                                                    ('os', '프로세스와 스레드의 차이를 설명해주세요.', '["독립된 메모리 영역(Code, Data, Stack, Heap)", "자원 공유", "Context Switching 오버헤드"]'::jsonb),
                                                                    ('os', '교착상태(Deadlock)의 발생 조건 4가지를 설명해주세요.', '["상호 배제(Mutual Exclusion)", "점유 대기", "비선점", "순환 대기(Circular Wait)"]'::jsonb),
                                                                    ('os', '페이징(Paging)과 세그멘테이션의 차이는 무엇인가요?', '["고정 크기", "가변 크기", "외부 단편화", "내부 단편화", "논리적 단위"]'::jsonb),
                                                                    ('os', 'CPU 스케줄링 알고리즘의 종류를 설명해주세요.', '["FCFS", "SJF", "Round Robin(시간 할당량)", "Priority", "Preemptive(선점/비선점)"]'::jsonb),
                                                                    ('os', '컨텍스트 스위칭(Context Switching)이란 무엇인가요?', '["PCB(Process Control Block)", "레지스터 상태 저장/복구", "오버헤드"]'::jsonb),
                                                                    ('os', '가상 메모리(Virtual Memory)의 개념을 설명해주세요.', '["물리 메모리 한계 극복", "요구 페이징(Demand Paging)", "페이지 테이블", "MMU"]'::jsonb),
                                                                    ('os', '인터럽트와 시스템 콜(System Call)의 차이는 무엇인가요?', '["하드웨어 발생", "소프트웨어 인터럽트", "커널 모드 전환", "권한 대행"]'::jsonb),
                                                                    ('os', '캐시 메모리의 지역성 원리를 설명해주세요.', '["시간 지역성(Temporal)", "공간 지역성(Spatial)", "적중률(Hit ratio)"]'::jsonb),
                                                                    ('os', '뮤텍스(Mutex)와 세마포어(Semaphore)의 차이를 설명해주세요.', '["상호 배제", "공유 자원 개수", "Binary Semaphore", "Locking/Unlocking"]'::jsonb),
                                                                    ('os', '프로세스 동기화가 필요한 이유를 설명해주세요.', '["임계 영역(Critical Section)", "데이터 일관성", "경쟁 상태(Race Condition)"]'::jsonb),

                                                                    ('network', 'TCP와 UDP의 차이점을 설명해주세요.', '["연결 지향", "신뢰성 보장", "흐름/혼잡 제어", "비연결성", "속도", "패킷 순서"]'::jsonb),
                                                                    ('network', 'HTTP와 HTTPS의 차이점을 설명해주세요.', '["SSL/TLS", "암호화", "포트 번호(80 vs 443)", "CA 인증서", "무결성"]'::jsonb),
                                                                    ('network', 'TCP 3-way handshake 과정을 설명해주세요.', '["SYN", "SYN-ACK", "ACK", "연결 확립(Establishment)"]'::jsonb),
                                                                    ('network', 'DNS(Domain Name System)의 동작 원리를 설명해주세요.', '["도메인 네임", "IP 주소 변환", "Recursive Query", "Authoritative Name Server"]'::jsonb),
                                                                    ('network', 'OSI 7계층 모델을 설명해주세요.', '["물리-데이터링크-네트워크-전송-세션-표현-응용", "캡슐화/역캡슐화"]'::jsonb),
                                                                    ('network', 'REST API의 특징과 HTTP 메서드를 설명해주세요.', '["Stateless(무상태)", "URI(자원)", "GET/POST/PUT/DELETE", "Uniform Interface"]'::jsonb),
                                                                    ('network', '쿠키(Cookie)와 세션(Session)의 차이를 설명해주세요.', '["클라이언트 저장", "서버 저장", "보안성 차이", "만료 시간", "브라우저 종료"]'::jsonb),
                                                                    ('network', 'CORS란 무엇이고 어떻게 해결하나요?', '["Same-Origin Policy(SOP)", "교차 출처", "HTTP 헤더(Access-Control-Allow-Origin)"]'::jsonb),
                                                                    ('network', '로드 밸런싱의 개념과 방식을 설명해주세요.', '["트래픽 분산", "L4/L7 스위치", "Round Robin", "가용성/확장성"]'::jsonb),
                                                                    ('network', 'WebSocket과 HTTP의 차이를 설명해주세요.', '["양방향 통신", "Full-duplex", "실시간성", "Handshake(업그레이드)"]'::jsonb),

                                                                    ('db', '데이터베이스 인덱스의 개념과 장단점을 설명해주세요.', '["검색 속도 향상", "B-Tree", "Full Scan 방지", "Insert/Update/Delete 성능 저하"]'::jsonb),
                                                                    ('db', '트랜잭션(Transaction)의 ACID 특성을 설명해주세요.', '["Atomicity(원자성)", "Consistency(일관성)", "Isolation(고립성)", "Durability(지속성)"]'::jsonb),
                                                                    ('db', 'SQL에서 JOIN의 종류와 차이점을 설명해주세요.', '["INNER JOIN", "LEFT/RIGHT OUTER JOIN", "교집합", "기준 테이블"]'::jsonb),
                                                                    ('db', '정규화(Normalization)의 목적과 단계를 설명해주세요.', '["데이터 중복 제거", "이상 현상(Anomaly) 방지", "무결성 유지", "테이블 분해"]'::jsonb),
                                                                    ('db', '캐시(Cache)의 개념과 사용 목적을 설명해주세요.', '["In-memory", "조회 성능", "Redis/Memcached", "데이터베이스 부하 감소"]'::jsonb),
                                                                    ('db', 'NoSQL과 RDBMS의 차이를 설명해주세요.', '["스키마(Schema)", "수평 확장(Scale-out)", "Key-Value/Document", "관계형(SQL)"]'::jsonb),
                                                                    ('db', '데이터베이스 락(Lock)의 종류를 설명해주세요.', '["공유 락(Shared)", "배타 락(Exclusive)", "동시성 제어", "Deadlock"]'::jsonb),
                                                                    ('db', 'B-Tree 인덱스의 동작 원리를 설명해주세요.', '["균형 트리(Balanced)", "노드 분할/병합", "로그 시간 복잡도 O(log n)"]'::jsonb),
                                                                    ('db', '파티셔닝과 샤딩(Sharding)의 차이를 설명해주세요.', '["수직/수평 분할", "물리적 서버 분산", "성능 최적화", "데이터 분산 저장"]'::jsonb),
                                                                    ('db', 'ORM의 장단점을 설명해주세요.', '["객체-관계 매핑", "생산성 향상", "SQL 추상화", "N+1 문제"]'::jsonb),

                                                                    ('ds', '스택(Stack)과 큐(Queue)의 차이를 설명해주세요.', '["LIFO(Last-In-First-Out)", "FIFO(First-In-First-Out)", "DFS", "BFS", "재귀"]'::jsonb),
                                                                    ('ds', '해시테이블 동작 원리와 충돌 해결 방법을 설명해주세요.', '["해시 함수", "Key-Value", "Chaining", "Open Addressing", "시간복잡도 O(1)"]'::jsonb),
                                                                    ('ds', '이진 탐색 트리(BST)의 특징과 시간복잡도를 설명해주세요.', '["왼쪽 자식 작음", "오른쪽 자식 큼", "편향 트리", "O(log n) ~ O(n)"]'::jsonb),
                                                                    ('ds', '그래프의 BFS와 DFS 탐색 차이를 설명해주세요.', '["큐(Queue)", "스택(Stack)/재귀", "최단 경로", "인접 리스트/행렬"]'::jsonb),
                                                                    ('ds', '힙(Heap) 자료구조의 특징과 사용 사례를 설명해주세요.', '["완전 이진 트리", "최댓값/최솟값", "우선순위 큐", "Heapify"]'::jsonb),
                                                                    ('ds', '동적 프로그래밍(DP)과 메모이제이션을 설명해주세요.', '["중복 계산 방지", "최적 부분 구조", "중복되는 부분 문제", "Bottom-up/Top-down"]'::jsonb),
                                                                    ('ds', '정렬 알고리즘 중 퀵정렬과 병합정렬을 비교해주세요.', '["Divide and Conquer", "Pivot", "안정 정렬(Stable)", "최악의 경우 O(n^2)"]'::jsonb),
                                                                    ('ds', '연결리스트와 배열(Array)의 차이를 설명해주세요.', '["연속된 메모리", "포인터", "인덱스 접근(Random Access)", "삽입/삭제 성능"]'::jsonb),
                                                                    ('ds', '트라이(Trie) 자료구조의 특징과 사용 사례를 설명해주세요.', '["문자열 검색", "접두사(Prefix)", "자동 완성", "메모리 사용량"]'::jsonb),
                                                                    ('ds', '시간복잡도 Big-O 표기법을 설명하고 예시를 들어주세요.', '["시간/공간 복잡도", "최악의 상황", "점근적 표기", "데이터 규모에 따른 증가율"]'::jsonb);

-- 면접 세션 데이터 삽입 (avg_duration을 180초로 세팅하여 DTO 변환 시 3분(avgTimeMin: 3)이 출력되도록 연동)
-- 개행문자(\n)를 주입하여 프론트엔드가 요구하는 다중 피드백 배열 파싱 규격을 완벽 지원
INSERT INTO interview_session (session_id, user_id, category, avg_score, overall_feedback, avg_duration, status, created_at) VALUES
    ('1e894e17-98ab-4bf2-8488-5ca0bd039826', 1, 'os', 84, '전반적으로 운영체제 기본 개념 이해도가 좋습니다.\n답변에 예시를 조금 더 추가하면 더 좋을 것 같습니다.', 180, 'COMPLETED', NOW());

-- 프론트엔드 시연 화면과 100% 매칭되는 개별 문항 결과 로그 적재
INSERT INTO answer_log (question_id, user_answer, ai_feedback, score, session_id, missing_keywords, duration, created_at) VALUES
                                                                                                                              (1, '프로세스는 독립된 메모리 공간을 가지고…', '프로세스와 스레드 차이를 잘 설명했습니다.', 88, 1, '["컨텍스트 스위칭"]'::jsonb, 35, NOW()),
                                                                                                                              (2, '상호 배제, 점유 대기, 비선점, 그리고 순환 대기 4가지가 있습니다.', '정확한 4가지 조건을 모두 잘 설명하셨습니다.', 100, 1, '[]'::jsonb, 40, NOW()),
                                                                                                                              (3, '페이징은 고정 크기로 나누고 외부 단편화를 해결합니다.', '핵심 차이를 짚었으나 내부 단편화 내용이 추가되면 좋겠습니다.', 85, 1, '["내부 단편화", "논리적 단위"]'::jsonb, 50, NOW()),
                                                                                                                              (4, 'FCFS, SJF, 라운드 로빈 등이 있습니다.', '주요 알고리즘을 언급했으나 선점형과 비선점형 구분이 부족합니다.', 70, 1, '["Priority", "Preemptive(선점/비선점)"]'::jsonb, 42, NOW()),
                                                                                                                              (5, 'CPU가 다른 프로세스로 넘어갈 때 상태를 저장/복구하는 과정입니다.', 'PCB와 상태 저장/복구 개념을 정확히 이해하고 있습니다.', 90, 1, '["오버헤드"]'::jsonb, 38, NOW()),
                                                                                                                              (6, '큰 프로그램을 실행하기 위해 사용하며, 페이지 테이블을 이용합니다.', '요구 페이징과 MMU에 대한 언급을 추가하면 완벽합니다.', 80, 1, '["요구 페이징(Demand Paging)", "MMU"]'::jsonb, 60, NOW()),
                                                                                                                              (7, '인터럽트는 하드웨어, 시스템 콜은 소프트웨어에서 커널을 호출합니다.', '정확한 비교입니다. 커널 모드 전환 설명도 좋습니다.', 95, 1, '["권한 대행"]'::jsonb, 47, NOW()),
                                                                                                                              (8, '최근에 사용된 데이터나 그 근처 데이터가 다시 사용될 확률입니다.', '시간/공간 지역성이라는 전문 용어를 사용하면 더 좋습니다.', 75, 1, '["시간 지역성(Temporal)", "공간 지역성(Spatial)", "적중률(Hit ratio)"]'::jsonb, 51, NOW()),
                                                                                                                              (9, '뮤텍스는 하나의 스레드만…', '기본 개념은 맞지만 활용 예시가 부족합니다.', 76, 1, '["임계영역"]'::jsonb, 40, NOW()),
                                                                                                                              (10, '여러 프로세스가 공유 자원에 접근할 때 일관성을 유지하기 위함입니다.', '경쟁 상태와 임계 영역이라는 키워드가 들어가면 완벽합니다.', 82, 1, '["임계 영역(Critical Section)", "경쟁 상태(Race Condition)"]'::jsonb, 47, NOW());

-- 복습 주기 알고리즘 테이블 매핑 데이터 생성
INSERT INTO review_state (user_id, question_id, repetition_count, easiness_factor, current_interval, next_review_date, last_reviewed_at) VALUES
                                                                                                                                             (1, 1, 1, 2.50, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 2, 1, 2.60, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 3, 1, 2.50, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 4, 0, 2.36, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 5, 1, 2.60, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 6, 1, 2.50, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 7, 1, 2.60, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 8, 0, 2.36, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 9, 1, 2.40, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE),
                                                                                                                                             (1, 10, 1, 2.50, 1, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE);