--
-- PostgreSQL database dump
--

\restrict Eznf0CTH93Uei6ldvHHwp91AwB7fwg8Ua7QT5phuEdC5bXMJPn7js6zqQwkSKpD

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

-- Started on 2026-05-17 16:09:58

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 224 (class 1259 OID 16418)
-- Name: question_keywords; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.question_keywords (
    id bigint NOT NULL,
    question_id bigint,
    keyword character varying(100) NOT NULL,
    weight smallint DEFAULT 1
);

ALTER TABLE public.question_keywords OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16417)
-- Name: question_keywords_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.question_keywords_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.question_keywords_id_seq OWNER TO postgres;

--
-- TOC entry 4964 (class 0 OID 0)
-- Dependencies: 223
-- Name: question_keywords_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.question_keywords_id_seq OWNED BY public.question_keywords.id;


--
-- TOC entry 222 (class 1259 OID 16400)
-- Name: questions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.questions (
    id bigint NOT NULL,
    subject_id bigint,
    title text NOT NULL,
    ideal_answer text NOT NULL,
    difficulty smallint DEFAULT 3
);


ALTER TABLE public.questions OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16399)
-- Name: questions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.questions_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.questions_id_seq OWNER TO postgres;

--
-- TOC entry 4965 (class 0 OID 0)
-- Dependencies: 221
-- Name: questions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.questions_id_seq OWNED BY public.questions.id;


--
-- TOC entry 220 (class 1259 OID 16388)
-- Name: subjects; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.subjects (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT check_allowed_subjects CHECK (((name)::text = ANY (ARRAY[('소프트웨어 설계/개발'::character varying)::text, ('DB구축'::character varying)::text, ('프로그래밍 언어 활용'::character varying)::text, ('정보시스템 구축 관리'::character varying)::text])))
);


ALTER TABLE public.subjects OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16387)
-- Name: subjects_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.subjects_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.subjects_id_seq OWNER TO postgres;

--
-- TOC entry 4966 (class 0 OID 0)
-- Dependencies: 219
-- Name: subjects_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.subjects_id_seq OWNED BY public.subjects.id;


--
-- TOC entry 226 (class 1259 OID 16483)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16482)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- TOC entry 4967 (class 0 OID 0)
-- Dependencies: 225
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- TOC entry 228 (class 1259 OID 16520)
-- Name: voice_learning_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.interview_session (
    id bigint NOT NULL,
    session_id character varying(36) NOT NULL,
    user_id bigint NOT NULL,
    subject character varying(100) NOT NULL,
    avg_score integer,
    overall_feedback text,
    avg_duration integer,
    status character varying(20) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.interview_session OWNER TO postgres;

CREATE SEQUENCE public.interview_session_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.interview_session_id_seq OWNER TO postgres;
ALTER SEQUENCE public.interview_session_id_seq OWNED BY public.interview_session.id;

CREATE TABLE public.answer_log (
    id bigint NOT NULL,
    question_id bigint NOT NULL,
    user_answer text NOT NULL,
    ai_feedback text NOT NULL,
    score integer,
    session_id bigint NOT NULL,
    missing_keywords character varying(255),
    matched_keywords character varying(255),
    captured_image_path character varying(255),
    duration integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

ALTER TABLE public.answer_log OWNER TO postgres;

CREATE SEQUENCE public.answer_log_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.answer_log_id_seq OWNER TO postgres;
ALTER SEQUENCE public.answer_log_id_seq OWNED BY public.answer_log.id;

CREATE TABLE public.review_state (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    question_id bigint NOT NULL,
    repetition_count integer NOT NULL,
    easiness_factor double precision NOT NULL,
    current_interval integer NOT NULL,
    next_review_date date NOT NULL,
    last_reviewed_at date NOT NULL
);

ALTER TABLE public.review_state OWNER TO postgres;

CREATE SEQUENCE public.review_state_id_seq
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_state_id_seq OWNER TO postgres;
ALTER SEQUENCE public.review_state_id_seq OWNED BY public.review_state.id;





--
-- TOC entry 4778 (class 2604 OID 16421)
-- Name: question_keywords id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.question_keywords ALTER COLUMN id SET DEFAULT nextval('public.question_keywords_id_seq'::regclass);


--
-- TOC entry 4776 (class 2604 OID 16403)
-- Name: questions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.questions ALTER COLUMN id SET DEFAULT nextval('public.questions_id_seq'::regclass);


--
-- TOC entry 4775 (class 2604 OID 16391)
-- Name: subjects id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects ALTER COLUMN id SET DEFAULT nextval('public.subjects_id_seq'::regclass);


--
-- TOC entry 4780 (class 2604 OID 16486)
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- TOC entry 4782 (class 2604 OID 16523)
-- Name: voice_learning_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.interview_session ALTER COLUMN id SET DEFAULT nextval('public.interview_session_id_seq'::regclass);

ALTER TABLE ONLY public.answer_log ALTER COLUMN id SET DEFAULT nextval('public.answer_log_id_seq'::regclass);

ALTER TABLE ONLY public.review_state ALTER COLUMN id SET DEFAULT nextval('public.review_state_id_seq'::regclass);


--
-- TOC entry 4954 (class 0 OID 16418)
-- Dependencies: 224
-- Data for Name: question_keywords; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.question_keywords (id, question_id, keyword, weight) FROM stdin;
1	1	추상화	1
2	1	캡슐화	1
3	1	상속	1
4	1	다형성	1
5	2	단일 책임	1
6	2	개방-폐쇄	1
7	2	리스코프 치환	1
8	2	인터페이스 분리	1
9	2	의존역전	1
10	3	인스턴스 하나만	1
11	3	디자인 패턴	1
12	3	동시성 문제	1
13	4	객체 생성	1
14	4	서브 클래스	1
15	4	인터페이스	1
16	4	객체 군	1
17	5	모델	1
18	5	뷰	1
19	5	컨트롤러	1
20	5	비즈니스 로직	1
21	6	의존성 주입	1
22	6	결합도 낮춤	1
23	6	유지보수	1
24	6	테스트 용이	1
25	7	실패하는 테스트	1
26	7	코드 작성	1
27	7	리팩토링	1
28	7	Red Green Refactor	1
29	8	도메인 모델	1
30	8	명확한 경계	1
31	8	Bounded Context	1
32	9	순차적	1
33	9	반복 주기	1
34	9	스프린트	1
35	9	피드백	1
36	10	master	1
37	10	develop	1
38	10	feature	1
39	10	브랜치 전략	1
40	11	가독성	1
41	11	의도 명확	1
42	11	유지보수성	1
43	12	기능 유지	1
44	12	내부 구조 개선	1
45	12	테스트 코드	1
46	13	정적 구조	1
47	13	클래스 관계	1
48	13	시각적 표현	1
49	14	알고리즘 교체	1
50	14	런타임	1
51	14	캡슐화	1
52	15	상태 변화	1
53	15	자동 통지	1
54	15	일대다 관계	1
55	16	호환성	1
56	16	인터페이스 변환	1
57	16	중간 매개	1
58	17	접근 제어	1
59	17	대리인	1
60	17	캐싱	1
61	17	지연 로딩	1
62	18	의사소통 기준	1
63	18	범위 확정	1
64	18	요구사항 명세	1
65	19	계층 분리	1
66	19	관심사 분리	1
67	19	수직적 구조	1
68	20	무상태성	1
69	20	클라이언트-서버	1
70	20	일관된 인터페이스	1
71	21	낮은 결합도	1
72	21	높은 응집도	1
73	21	Loose Coupling	1
74	22	독립적 단위	1
75	22	복잡도 낮춤	1
76	22	재사용성	1
77	23	품질 유지	1
78	23	지식 공유	1
79	23	버그 발견	1
80	24	지속적 통합	1
81	24	지속적 배포	1
82	24	자동화 빌드	1
83	25	실행하지 않고	1
84	25	실행하며	1
85	25	잠재 결함	1
86	26	가장 작은 단위	1
87	26	메서드 검증	1
88	26	독립적 테스트	1
89	27	모듈 간 상호작용	1
90	27	인터페이스 검증	1
91	27	유기적 동작	1
92	28	사용자 관점	1
93	28	기능 요구사항	1
94	28	시나리오	1
95	29	독립 배포	1
96	29	복잡도 증가	1
97	29	서비스 분할	1
98	30	데이터 전송	1
99	30	불변 객체	1
100	30	값 표현	1
101	31	내부 상태	1
102	31	행위 변경	1
103	31	조건문 제거	1
104	32	생성 과정 분리	1
105	32	가독성 향상	1
106	32	선택적 매개변수	1
107	33	구체적 동작	1
108	33	성능과 보안	1
109	33	제약조건	1
110	34	수정 전파	1
111	34	유지보수 저하	1
112	34	경직성	1
113	35	변경 취약	1
114	35	후반부 발견	1
115	35	유연성 부족	1
116	36	예외 상황 방지	1
117	36	일관된 검증	1
118	36	테스트 입력값	1
119	37	독립 모듈	1
120	37	조합	1
121	37	재사용 극대화	1
122	38	내부 코드 구조	1
123	38	기능 위주	1
124	38	입출력 검증	1
125	39	복잡도 폭발	1
126	39	일정 지연	1
127	39	품질 저하	1
128	40	비즈니스 핵심 규칙	1
129	40	개념 구조 단순화	1
130	40	전문가 협업	1
131	41	검색 속도 향상	1
132	41	B-Tree	1
133	41	성능 저하	1
134	42	Atomicity	1
135	42	원자성	1
136	42	Consistency	1
137	42	일관성	1
138	42	Isolation	1
139	42	고립성	1
140	42	Durability	1
141	42	지속성	1
142	43	INNER JOIN	1
143	43	LEFT/RIGHT OUTER JOIN	1
144	43	기준 테이블	1
145	44	데이터 중복 제거	1
146	44	이상 현상	1
147	44	Anomaly	1
148	44	테이블 분해	1
149	45	In-memory	1
150	45	조회 성능	1
151	45	Redis	1
152	45	부하 감소	1
153	46	스키마	1
154	46	수평 확장	1
155	46	Scale-out	1
156	46	관계형	1
157	47	공유 락	1
158	47	배타 락	1
159	47	동시성 제어	1
160	48	균형 트리	1
161	48	노드 분할	1
162	48	O(log n)	1
163	49	수직/수평 분할	1
164	49	물리적 서버 분산	1
165	49	성능 최적화	1
166	50	객체-관계 매핑	1
167	50	생산성 향상	1
168	50	N+1 문제	1
169	51	개체 무결성	1
170	51	참조 무결성	1
171	51	도메인 무결성	1
172	52	유일하게 식별	1
173	52	참조 관계	1
174	53	Read Committed	1
175	53	Repeatable Read	1
176	53	Serializable	1
177	54	완료되지 않은	1
178	54	변경 데이터	1
179	54	읽는 현상	1
180	55	타임아웃	1
181	55	롤백	1
182	55	락 순서	1
183	56	삽입 이상	1
184	56	삭제 이상	1
185	56	갱신 이상	1
186	57	이행적 함수 종속	1
187	57	제2정규형 만족	1
188	58	모든 결정자	1
189	58	후보키	1
190	59	조회 성능	1
191	59	데이터 중복 허용	1
192	60	가상 테이블	1
193	60	보안 제공	1
194	60	쿼리 단순화	1
195	61	물리적 정렬	1
196	61	넌클러스터드	1
197	62	파일 형태 추출	1
198	62	전체 시스템 복사	1
199	63	영구 반영	1
200	63	시작 전 상태	1
201	64	실행 속도	1
202	64	로직 분산	1
203	64	네트워크 트래픽	1
204	65	자동 실행	1
205	65	연쇄 반응	1
206	65	성능 저하	1
207	66	그룹화	1
208	66	그룹 조건	1
209	67	스칼라	1
210	67	인라인 뷰	1
211	67	중첩	1
212	68	의사결정	1
213	68	통합 데이터	1
214	68	분석용	1
215	69	변경 로그	1
216	69	디스크에 먼저	1
217	70	인덱스	1
218	70	Full Scan	1
219	70	성능 최적화	1
220	71	일관성	1
221	71	가용성	1
222	71	분할 내성	1
223	72	한 행씩	1
224	72	성능 저하	1
225	72	포인터	1
226	73	외부 스키마	1
227	73	개념 스키마	1
228	73	내부 스키마	1
229	74	절차적	1
230	74	비절차적	1
231	75	메타데이터	1
232	75	시스템 테이블	1
233	76	버전 체크	1
234	76	실제 데이터 락	1
235	76	낙관적	1
236	77	미리 생성	1
237	77	오버헤드 줄임	1
238	77	재사용	1
239	78	Update	1
240	78	Insert	1
241	79	역색인	1
242	79	형태소 분석	1
243	79	고속 검색	1
244	80	준비 단계	1
245	80	커밋 단계	1
246	80	원자적	1
247	81	LIFO	1
248	81	FIFO	1
249	81	DFS	1
250	81	BFS	1
251	82	해시 함수	1
252	82	Chaining	1
253	82	Open Addressing	1
254	82	O(1)	1
255	83	왼쪽 자식 작음	1
256	83	오른쪽 자식 큼	1
257	83	O(log n)	1
258	84	큐	1
259	84	스택	1
260	84	재귀	1
261	84	최단 경로	1
262	85	완전 이진 트리	1
263	85	최댓값	1
264	85	우선순위 큐	1
265	86	중복 계산 방지	1
266	86	메모이제이션	1
267	86	하위 문제	1
268	87	Divide and Conquer	1
269	87	Pivot	1
270	87	안정 정렬	1
271	88	연속된 메모리	1
272	88	포인터	1
273	88	Random Access	1
274	88	삽입/삭제	1
275	89	문자열 검색	1
276	89	접두사	1
277	89	Prefix	1
278	89	자동 완성	1
279	90	최악의 상황	1
281	90	증가율	1
282	91	힙 메모리	1
283	91	자동 해제	1
284	91	참조	1
285	92	클래스 영역	1
286	92	힙 영역	1
287	92	스택 영역	1
288	93	다중 상속 불가	1
289	93	다중 구현	1
290	93	추상 메서드	1
291	94	이름은 같고	1
292	94	매개변수 다름	1
293	94	재정의	1
294	95	값 복사	1
295	95	참조 주소값 복사	1
296	96	주소 비교	1
297	96	값 비교	1
298	97	불변	1
299	97	동기화 지원	1
300	97	StringBuilder 고속	1
301	98	순서 허용	1
302	98	중복 불가	1
303	98	Key-Value	1
304	99	Thread 상속	1
305	99	Runnable 구현	1
306	100	컴파일 시점	1
307	100	런타임	1
308	100	예외 처리 필수	1
309	101	익명 함수	1
310	101	코드가 간결	1
311	102	컴파일 시점 타입	1
312	102	타입 안전성	1
313	103	원본 변경 안함	1
314	103	파이프라인	1
315	104	동시 접근	1
316	104	정합성 유지	1
317	104	Thread-safe	1
318	105	런타임 시점	1
319	105	메타데이터 분석	1
320	106	기계어 번역	1
321	106	한 줄씩 해석	1
322	107	컴파일 타입 체크	1
323	107	유연성	1
324	108	참조 고리	1
325	108	해제하지 못함	1
326	109	외부 함수 종료	1
327	109	변수 환경 접근	1
328	110	정렬되어 있어야	1
329	110	O(log n)	1
330	111	연결 리스트	1
331	111	버킷	1
332	112	매 순간 최선	1
333	112	전체 최적해 아님	1
334	113	이진수 단위	1
335	113	메모리 절약	1
336	114	상속 불가	1
337	114	오버라이딩 불가	1
338	114	상수	1
339	115	구간 합	1
340	115	O(log n)	1
341	116	기존 순서 유지	1
342	116	상대적 순서	1
343	117	가장 가까운 곳	1
344	117	거리 갱신	1
345	118	바이너리 변환	1
346	118	객체 조립	1
347	119	작업 완료 대기	1
348	119	즉시 다음 코드	1
349	120	우선순위	1
350	120	라운드 로빈	1
351	120	시분할	1
352	121	독립된 메모리 영역	1
353	121	자원 공유	1
354	121	Context Switching	1
355	122	상호 배제	1
356	122	점유 대기	1
357	122	비선점	1
358	122	순환 대기	1
359	123	고정 크기	1
360	123	가변 크기	1
361	123	외부 단편화	1
362	123	내부 단편화	1
363	124	FCFS	1
364	124	SJF	1
365	124	Round Robin	1
366	124	선점	1
367	124	비선점	1
368	125	PCB	1
369	125	상태 저장	1
370	125	복구	1
371	126	물리 메모리 한계	1
372	126	요구 페이징	1
373	126	MMU	1
374	127	하드웨어 발생	1
375	127	커널 모드 전환	1
376	127	권한 대행	1
377	128	시간 지역성	1
378	128	공간 지역성	1
379	128	적중률	1
380	129	상호 배제	1
381	129	공유 자원 개수	1
382	129	Locking	1
383	130	임계 영역	1
384	130	데이터 일관성	1
385	130	경쟁 상태	1
386	131	같은 키	1
387	131	다른 키	1
388	131	속도	1
389	132	물리-데이터링크-네트워크-전송-세션-표현-응용	1
390	132	패킷	1
391	132	세그먼트	1
392	133	연결 확립	1
393	133	비연결성	1
394	133	흐름 제어	1
395	134	SSL/TLS	1
396	134	443 포트	1
397	134	암호화 전송	1
398	135	리커시브	1
399	135	루트 네임서버	1
400	135	IP 반환	1
401	136	로컬 브라우저	1
402	136	서버 메모리	1
403	136	변조 위험	1
404	137	교차 출처	1
405	137	브라우저 차단	1
406	137	서버 헤더 허용	1
407	138	라운드 로빈	1
408	138	최소 연결	1
409	138	해시	1
410	139	양방향	1
411	139	실시간 스트리밍	1
412	139	오버헤드 없이	1
413	140	좀비 PC	1
414	140	과도한 트래픽	1
415	140	마비	1
416	141	SQL 구문 주입	1
417	141	DB 조작	1
418	141	PreparedStatement	1
419	142	자바스크립트 코드 삽입	1
420	142	토큰 탈취	1
421	142	XSS	1
422	143	IP/포트 차단	1
423	143	침입 탐지	1
424	143	실시간 방어	1
425	144	블록 암호화	1
426	144	DES 대체	1
427	144	AES	1
428	145	소수	1
429	145	소인수분해	1
430	145	수학적 복잡성	1
431	146	신원 증명	1
432	146	메시지 변조	1
433	146	부인 방지	1
434	147	권한 토큰	1
435	147	Access Token	1
436	147	인증 위임	1
437	148	호스트 커널 공유	1
438	148	경량화	1
439	148	배포 속도	1
440	149	배포 및 스케일링	1
441	149	자동 복구	1
442	149	오케스트레이션	1
443	150	가상 인프라	1
444	150	개발 플랫폼	1
445	150	완성된 소프트웨어	1
446	151	가상머신	1
447	151	VM	1
448	151	자원 분배	1
449	152	다른 네트워크 연결	1
450	152	최적 경로	1
451	152	라우팅	1
452	153	작은 네트워크	1
453	153	IP 주소 낭비	1
454	153	브로드캐스트 부하	1
455	154	자동 할당	1
456	154	DHCP	1
457	155	스트라이핑	1
458	155	미러링	1
459	155	안정성	1
460	156	기밀성	1
461	156	무결성	1
462	156	가용성	1
463	157	공유기 특정 포트	1
464	157	내부망 기기	1
465	157	요청 토스	1
466	158	I/O Wait	1
467	158	디스크 대기	1
468	158	병목 현상	1
469	159	임계 경로	1
470	159	가장 긴 작업 경로	1
471	159	일정 지연	1
472	160	서버 상태 저장	1
473	160	토큰 자체 정보	1
474	160	무상태 서버	1
280	90	점근적 표기	1
475	54	더티 리드	1
\.


--
-- TOC entry 4952 (class 0 OID 16400)
-- Dependencies: 222
-- Data for Name: questions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.questions (id, subject_id, title, ideal_answer, difficulty) FROM stdin;
1	1	객체 지향 프로그래밍(OOP)의 4대 특징을 설명하세요.	추상화, 캡슐화, 상속, 다형성이 있습니다.	3
2	1	SOLID 원칙에 대해 설명해주세요.	단일 책임, 개방-폐쇄, 리스코프 치환, 인터페이스 분리, 의존역전 원칙입니다.	3
3	1	싱글톤 패턴(Singleton Pattern)의 개념과 주의점을 말해주세요.	인스턴스를 하나만 생성하는 패턴이며 멀티스레드 동시성 문제를 주의해야 합니다.	3
4	1	팩토리 메서드 패턴과 추상 팩토리 패턴의 차이는 무엇인가요?	팩토리는 메서드 수준에서 객체를 생성하고, 추상 팩토리는 관련된 객체 군을 통째로 생성합니다.	3
5	1	MVC 패턴의 각 컴포넌트 역할을 설명하세요.	데이터를 다루는 모델, 화면을 보여주는 뷰, 로직을 제어하는 컨트롤러로 나뉩니다.	3
6	1	의존성 주입(DI)의 개념과 장점을 설명하세요.	외부에서 객체를 주입받아 결합도를 낮추고 테스트 용이성을 높이는 기법입니다.	3
7	1	TDD(테스트 주도 개발)의 사이클에 대해 설명하세요.	실패하는 테스트를 먼저 작성하고, 이를 통과하는 코드를 짠 뒤 리팩토링합니다.	3
8	1	DDD(도메인 주도 설계)에서 바운디드 컨텍스트란 무엇인가요?	특정 도메인 모델이 적용되는 명확한 경계를 의미합니다.	3
9	1	애자일(Agile) 방법론과 워터폴 방법론의 차이는 무엇인가요?	순차적으로 진행되는 워터폴과 달리 애자일은 짧은 주기의 반복을 통해 요구사항을 반영합니다.	3
10	1	형상 관리 도구에서 Git의 Git Flow 전략을 설명하세요.	master, develop, feature, release, hotfix 브랜치를 활용해 관리하는 전략입니다.	3
11	1	클린 코드(Clean Code)에서 강조하는 가독성의 중요성을 설명하세요.	코드는 작성되는 시간보다 읽히는 시간이 많으므로 의도가 명확해야 합니다.	3
12	1	리팩토링(Refactoring)의 목적과 주의점을 설명하세요.	기능은 유지한 채 내부 구조를 개선하는 것이며, 테스트 코드가 뒷받침되어야 합니다.	3
13	1	UML 다이어그램 중 클래스 다이어그램의 용도를 설명하세요.	시스템의 정적 구조와 클래스 간의 관계를 시각적으로 표현합니다.	3
14	1	디자인 패턴 중 전략 패턴(Strategy Pattern)은 언제 쓰나요?	실행 중에 알고리즘을 유연하게 바꾸어 적용하고 싶을 때 사용합니다.	3
15	1	옵저버 패턴(Observer Pattern)의 개념을 설명하세요.	한 객체의 상태 변화를 관찰자들에게 자동 통지하는 패턴입니다.	3
16	1	어댑터 패턴(Adapter Pattern)의 역할을 설명하세요.	호환되지 않는 인터페이스들을 함께 작동할 수 있도록 연결해 줍니다.	3
17	1	프록시 패턴(Proxy Pattern)의 장점을 설명하세요.	실제 객체에 접근하기 전 제어 흐름을 가로채어 권한 검사나 캐싱을 수행합니다.	3
18	1	요구사항 명세서(SRS) 작성이 중요한 이유를 설명하세요.	개발자와 고객 간의 의사소통 기준이 되며 프로젝트의 범위를 확정 짓기 때문입니다.	3
19	1	소프트웨어 아키텍처 패턴 중 레이어드 아키텍처를 설명하세요.	관심사 분리를 위해 시스템을 수직적인 계층(프레젠테이션, 비즈니스, 데이터)으로 나눕니다.	3
21	1	결합도(Coupling)와 응집도(Cohesion)의 이상적인 관계는 무엇인가요?	낮은 결합도와 높은 응집도를 유지하는 것이 가장 이상적입니다.	3
22	1	모듈화(Modularity)의 개념과 효과를 설명하세요.	소프트웨어를 독립적인 기능을 가진 단위로 나누어 복잡도를 낮추고 재사용성을 높이는 것입니다.	3
23	1	코드 리뷰(Code Review)의 주된 목적을 설명하세요.	버그를 조기 발견하고 지식을 공유하며 전체적인 코드 품질과 표준을 유지하기 위함입니다.	3
24	1	CI/CD 파이프라인의 개념을 설명하세요.	지속적 통합과 지속적 배포를 통해 코드 변경사항을 자동으로 빌드, 테스트, 반영하는 시스템입니다.	3
25	1	정적 분석 도구와 동적 분석 도구의 차이는 무엇인가요?	정적 분석은 코드를 실행하지 않고 잠재적 결함을 찾고, 동적 분석은 코드를 실행하며 분석합니다.	3
26	1	단위 테스트(Unit Test)의 정의를 설명하세요.	소스 코드의 가장 작은 단위(주로 메서드)가 예상대로 동작하는지 독립적으로 검증하는 테스트입니다.	3
27	1	통합 테스트(Integration Test)는 무엇을 검증하나요?	모듈 간의 상호작용과 인터페이스가 정상적으로 유기 동작하는지 검증합니다.	3
28	1	유스케이스(Usecase) 기술서의 목적을 설명하세요.	사용자 관점에서 시스템이 제공해야 하는 기능적 요구사항을 시나리오 형태로 정립하는 것입니다.	3
29	1	마이크로서비스 아키텍처(MSA)의 장단점을 설명하세요.	독립적 배포와 확장이 가능하지만 시스템 복잡도가 증가하고 트랜잭션 관리가 어렵습니다.	3
30	1	DTO(Data Transfer Object)와 VO(Value Object)의 차이를 설명하세요.	DTO는 가변 객체로 데이터 전송 목적이며, VO는 불변 객체로 값을 표현하는 목적입니다.	3
31	1	상태 패턴(State Pattern)은 언제 활용되나요?	객체의 내부 상태에 따라 행위를 완전히 다르게 바꾸어야 할 때 사용됩니다.	3
32	1	빌더 패턴(Builder Pattern)을 사용하는 장점을 설명하세요.	복잡한 객체의 생성 과정을 단계별로 유연하게 처리하고 가독성을 높입니다.	3
33	1	기능 요구사항과 비기능 요구사항의 차이는 무엇인가요?	기능 요구사항은 시스템의 구체적인 동작이며, 비기능은 성능, 보안, 가용성 같은 제약조건입니다.	3
34	1	객체 간의 결합도가 높을 때 발생하는 문제점을 설명하세요.	코드 한 곳을 수정했을 때 연관된 다른 모듈들이 무더기로 깨져 유지보수가 극도로 힘들어집니다.	3
35	1	폭포수 모델(Waterfall)의 치명적인 단점은 무엇인가요?	초기 요구사항 정의가 완벽해야 하며, 개발 후반부에 요구사항 변경이 발생하면 대처가 불가능합니다.	3
36	1	테스트 케이스(Test Case)를 작성하는 이유를 설명하세요.	개발자가 미처 파악하지 못한 예외 상황을 방지하고 입력값에 따른 출력을 일관되게 검증하기 위함입니다.	3
37	1	컴포넌트(Component) 기반 개발의 특징을 설명하세요.	독립적인 비즈니스 기능을 수행하는 모듈을 조합하여 시스템을 구축함으로써 재사용성을 극대화합니다.	3
38	1	화이트박스 테스트와 블랙박스 테스트의 차이를 설명하세요.	화이트박스는 내부 코드 구조를 보며 테스트하고, 블랙박스는 구조를 모른 채 기능 위주로 테스트합니다.	3
39	1	소프트웨어 위기(Software Crisis)의 주된 요인을 설명하세요.	하드웨어 발전에 비해 소프트웨어의 규모와 복잡도가 폭발하여 일정 지연 및 품질 저하가 발생한 현상입니다.	3
40	1	도메인 모델(Domain Model)을 설계하는 목적을 설명하세요.	비즈니스 핵심 규칙과 개념적 구조를 단순화하여 개발자와 도메인 전문가 간의 격차를 해소하기 위함입니다.	3
41	2	데이터베이스 인덱스(Index)의 개념과 장단점을 설명해주세요.	검색 속도 향상을 위해 구조를 사용하지만 삽입, 수정, 삭제 작업 시 성능이 저하될 수 있습니다.	3
42	2	트랜잭션(Transaction)의 ACID 특성을 설명해주세요.	원자성, 일관성, 고립성, 지속성을 통해 안전성을 보장합니다.	3
43	2	SQL에서 JOIN의 종류와 차이점을 설명해주세요.	INNER JOIN, LEFT/RIGHT OUTER JOIN 등이 있습니다.	3
44	2	정규화(Normalization)의 목적과 단계를 설명해주세요.	데이터 중복을 제거하고 이상 현상을 방지하기 위해 테이블을 단계별로 분해하는 과정입니다.	3
45	2	캐시(Cache)의 개념과 사용 목적을 설명해주세요.	In-memory 데이터 보관을 통해 조회 성능을 향상시키고 DB 부하를 줄입니다.	3
46	2	NoSQL과 RDBMS의 차이를 설명해주세요.	정형화된 스키마 기반의 RDBMS와 유연한 확장성을 가진 NoSQL의 구조적 차이입니다.	3
47	2	데이터베이스 락(Lock)의 종류를 설명해주세요.	동시성 제어를 위해 사용하는 공유 락과 배타 락이 있습니다.	3
48	2	B-Tree 인덱스의 동작 원리를 설명해주세요.	균형 트리를 유지하며 노드 분할과 병합을 통해 로그 시간 복잡도의 탐색을 제공합니다.	3
49	2	파티셔닝(Partitioning)과 샤딩(Sharding)의 차이를 설명해주세요.	단일 DB 내부 분할인 파티셔닝과 물리적 서버 분산인 샤딩의 차이입니다.	3
50	2	ORM(Object-Relational Mapping)의 장단점을 설명해주세요.	객체-관계 자동 매핑으로 생산성이 높지만 복잡한 쿼리 시 N+1 문제가 발생할 수 있습니다.	3
51	2	데이터 무결성(Integrity)의 종류를 설명하세요.	개체 무결성, 참조 무결성, 도메인 무결성 등이 있습니다.	3
52	2	기본키(Primary Key)와 외래키(Foreign Key)의 역할을 설명하세요.	기본키는 행을 유일하게 식별하고, 외래키는 다른 테이블과의 참조 관계를 맺어줍니다.	3
53	2	트랜잭션 격리 수준(Isolation Level)의 종류를 말해주세요.	Read Uncommitted, Read Committed, Repeatable Read, Serializable이 있습니다.	3
54	2	Dirty Read 현상이란 무엇인가요?	한 트랜잭션이 완료되지 않은 다른 트랜잭션의 변경 데이터를 읽는 현상입니다.	3
55	2	데드락(Deadlock)이 DB에서 발생할 때 해결 방법을 설명하세요.	트랜잭션 타임아웃 설정, 락 순서 고정, 혹은 DB 데드락 디텍터가 한쪽을 롤백시킵니다.	3
56	2	이상 현상(Anomaly)의 3가지 종류를 설명하세요.	삽입 이상, 삭제 이상, 갱신 이상이 있습니다.	3
57	2	제3정규형(3NF)의 조건을 설명하세요.	제2정규형을 만족하고, 이행적 함수 종속(X->Y, Y->Z)을 제거해야 합니다.	3
58	2	BCNF(보이스-코드 정규형)는 어떤 조건인가요?	제3정규형을 만족하고, 모든 결정자가 후보키여야 합니다.	3
59	2	역정규화(Denormalization)를 하는 이유를 설명하세요.	과도한 조인으로 인해 저하된 시스템의 조회 성능을 향상시키기 위함입니다.	3
60	2	뷰(View)의 개념과 장점을 설명하세요.	가상의 테이블로, 복잡한 쿼리를 단순화하고 원본 데이터에 대한 보안을 제공합니다.	3
61	2	클러스터드 인덱스와 넌클러스터드 인덱스의 차이를 설명하세요.	클러스터드는 실제 데이터의 물리적 정렬 순서와 같고, 넌클러스터드는 별도의 페이지로 인덱스를 구성합니다.	3
62	2	데이터베이스 덤프(Dump)와 백업(Backup)의 차이는 무엇인가요?	덤프는 파일 형태로 데이터를 추출하는 것이며, 백업은 전체 시스템 상태를 복사해 두는 것입니다.	3
63	2	커밋(Commit)과 롤백(Rollback)의 연산을 설명하세요.	커밋은 변경사항을 영구 반영하고, 롤백은 트랜잭션 시작 전 상태로 되돌립니다.	3
64	2	Stored Procedure 사용의 장단점을 설명하세요.	네트워크 트래픽을 줄이고 실행 속도가 빠르지만, 로직 분산으로 유지보수가 힘들어집니다.	3
65	2	Trigger의 개념과 남용 시 문제점을 설명하세요.	이벤트 발생 시 자동 실행되는 코드로, 남용하면 예측하기 힘든 연쇄 반응으로 성능이 저하됩니다.	3
66	2	GROUP BY와 HAVING의 차이를 설명하세요.	GROUP BY는 데이터를 그룹화하고, HAVING은 그룹화된 결과에 필터 조건을 겁니다.	3
67	2	서브쿼리(Subquery)의 종류를 설명하세요.	스칼라 서브쿼리, 인라인 뷰, 중첩 서브쿼리가 있습니다.	3
68	2	데이터 웨어하우스(Data Warehouse)란 무엇인가요?	의사결정을 위해 여러 시스템의 통합 데이터를 누적 관리하는 분석용 데이터베이스입니다.	3
69	2	WAL(Write-Ahead Logging)의 원리를 설명하세요.	데이터를 변경하기 전, 변경 로그를 먼저 디스크에 안전하게 기록하는 기법입니다.	3
70	2	Execution Plan(실행 계획)을 확인하는 이유를 설명하세요.	작성한 SQL이 인덱스를 타는지, Full Scan을 하는지 확인하여 성능을 최적화하기 위함입니다.	3
71	2	CAP 이론에 대해 설명하세요.	분산 시스템은 일관성, 가용성, 분할 내성 중 최대 2가지만 동시에 만족할 수 있다는 이론입니다.	3
72	2	커서(Cursor)의 개념과 단점을 설명하세요.	쿼리 결과 집합을 한 행씩 처리하는 포인터로, 대량 데이터 처리 시 성능이 매우 저하됩니다.	3
73	2	데이터베이스 스키마(Schema)의 3단계 구조를 설명하세요.	외부 스키마, 개념 스키마, 내부 스키마로 구성됩니다.	3
74	2	관계 대수와 관계 해석의 차이는 무엇인가요?	관계 대수는 절차적 언어이고, 관계 해석은 비절차적 언어입니다.	3
75	2	데이터 사전(Data Dictionary)이란 무엇인가요?	데이터베이스 자체의 메타데이터(테이블 정보, 권한 등)를 담고 있는 시스템 테이블입니다.	3
77	2	Connection Pool을 사용하는 이유를 설명하세요.	미리 커넥션을 생성해 두고 재사용함으로써 DB 연결 생성 오버헤드를 줄이기 위함입니다.	3
78	2	UPSERT 연산이란 무엇인가요?	데이터가 있으면 Update를 수행하고, 없으면 Insert를 수행하는 연산입니다.	3
79	2	Elasticsearch와 같은 검색 엔진을 DB와 함께 쓰는 이유를 설명하세요.	RDBMS가 취약한 대량 텍스트의 형태소 분석 및 역색인 기반 고속 검색을 지원하기 위함입니다.	3
80	2	데이터 분산 저장 환경에서 2단계 커밋(2PC)이란 무엇인가요?	모든 노드가 트랜잭션을 원자적으로 처리하도록 준비 단계와 커밋 단계로 나누어 제어하는 프로토콜입니다.	3
81	3	스택(Stack)과 큐(Queue)의 차이를 설명하고 활용 예시를 들어주세요.	LIFO 구조의 스택과 FIFO 구조의 큐의 동작 방식 및 알고리즘 활용 차이입니다.	3
82	3	해시테이블(Hash Table)의 동작 원리와 충돌 해결 방법을 설명해주세요.	해시 함수를 활용하며 충돌 발생 시 Chaining이나 Open Addressing을 사용합니다.	3
83	3	이진 탐색 트리(BST)의 특징과 시간복잡도를 설명해주세요.	왼쪽 자식은 작고 오른쪽 자식은 큰 트리 구조로 평균 탐색 속도는 O(log n)입니다.	3
84	3	그래프의 BFS와 DFS 탐색 차이를 설명해주세요.	인접 노드 우선의 큐 기반 BFS와 깊이 우선의 스택/재귀 기반 DFS의 차이입니다.	3
85	3	힙(Heap) 자료구조의 특징과 사용 사례를 설명해주세요.	완전 이진 트리로 최댓값/최솟값을 고속으로 찾기 위해 우선순위 큐 등에 쓰입니다.	3
86	3	동적 프로그래밍(DP)의 개념과 메모이제이션을 설명해주세요.	중복 계산 방지를 위해 하위 문제의 답을 기록하고 Top-down/Bottom-up 방식으로 풉니다.	3
87	3	정렬 알고리즘 중 퀵정렬과 병합정렬을 비교해주세요.	피벗을 기준으로 분할 정복하는 퀵정렬과 안정 정렬인 병합정렬의 속도 차이입니다.	3
88	3	연결리스트(LinkedList)와 배열(Array)의 차이를 설명해주세요.	인덱스 무작위 접근이 빠른 배열과 포인터를 연결하여 삽입/삭제가 용이한 연결리스트입니다.	3
89	3	트라이(Trie) 자료구조의 특징과 사용 사례를 설명해주세요.	접두사를 계층형으로 표현하여 문자열 검색 및 자동 완성 기능을 구현할 때 유용합니다.	3
90	3	시간복잡도 Big-O 표기법을 설명하고 예시를 들어주세요.	알고리즘의 최악의 상황 실행 속도를 데이터 규모 증가율 기준으로 나타낸 지표입니다.	3
91	3	자바의 가비지 컬렉션(GC)의 역할과 원리를 설명하세요.	힙 메모리 영역에서 참조되지 않는 객체들을 자동으로 해제하는 역할을 합니다.	3
92	3	자바의 JVM 구조와 메모리 영역(Runtime Data Area)을 설명하세요.	자바 코드를 실행하는 가상머신으로 클래스, 메서드, 힙, 스택, PC 레지스터 영역이 있습니다.	3
93	3	추상 클래스(Abstract Class)와 인터페이스(Interface)의 차이는 무엇인가요?	추상 클래스는 다중 상속이 불가능하고 상태를 가질 수 있으며, 인터페이스는 다중 구현이 목적입니다.	3
94	3	오버로딩(Overloading)과 오버라이딩(Overriding)의 차이를 설명하세요.	오버로딩은 메서드 이름은 같고 매개변수를 다르게 정의하는 것이고, 오버라이딩은 상속받은 메서드를 재정의하는 것입니다.	3
95	3	자바의 Call by Value와 Call by Reference의 동작을 설명하세요.	자바는 항상 값을 복사해 넘기는 Call by Value 방식으로 동작하며 참조 주소값 자체를 복사합니다.	3
96	3	Equals()와 == 연산자의 차이를 설명하세요.	== 연산자는 메모리 주소를 비교하고, equals() 메서드는 객체의 실제 내부 값을 비교합니다.	3
97	3	String, StringBuffer, StringBuilder의 차이를 설명하세요.	String은 불변 객체이며, StringBuffer는 멀티스레드 동기화를 지원하는 가변 객체이고, StringBuilder는 동기화가 없는 고속 가변 객체입니다.	3
98	3	자바의 컬렉션 프레임워크 중 List, Set, Map의 특징을 말해주세요.	List는 순서가 있고 중복을 허용하며, Set은 중복을 허용하지 않고, Map은 Key-Value 쌍으로 데이터를 저장합니다.	3
99	3	스레드(Thread)를 자바에서 구현하는 두 가지 방법을 설명하세요.	Thread 클래스를 상속받거나, Runnable 인터페이스를 구현하는 방법이 있습니다.	3
100	3	자바의 예외 처리(Exception) 중 Checked와 Unchecked의 차이를 설명하세요.	Checked는 컴파일 시점에 체크되어 예외 처리가 필수적이며, Unchecked는 런타임에 발생합니다.	3
101	3	함수형 프로그래밍과 람다식(Lambda Expression)의 장점을 설명하세요.	익명 함수를 사용해 코드를 간결하게 만들고 가독성을 높이며 부작용을 줄입니다.	3
102	3	자바의 제네릭(Generic)의 개념과 사용 목적을 설명하세요.	데이터 타입을 컴파일 시점에 고정하여 타입 안전성을 높이고 불필요한 형변환을 제거하기 위함입니다.	3
103	3	스트림 API(Stream API)의 특징을 설명하세요.	원본 데이터를 변경하지 않고 선언형 코드로 데이터를 필터링, 매핑, 정렬하는 파이프라인 연산을 제공합니다.	3
104	3	멀티스레드 환경에서 Thread-safe 하다는 의미는 무엇인가요?	여러 스레드가 동일한 자원에 동시 접근하더라도 데이터 정합성이 깨지지 않고 안전하게 유지되는 상태입니다.	3
105	3	자바의 Reflection API란 무엇인가요?	런타임 시점에 클래스의 메타데이터(메서드, 필드 등)를 동적으로 분석하고 제어할 수 있게 해주는 기능입니다.	3
106	3	컴파일러 언어와 인터프리터 언어의 차이를 설명하세요.	컴파일러는 기계어로 일괄 변환 후 실행하여 빠르고, 인터프리터는 한 줄씩 해석하며 실행하므로 빌드 속도는 빠릅니다.	3
107	3	정적 타입 언어와 동적 타입 언어의 장단점을 설명하세요.	정적 타입은 컴파일 시 타입 체크로 안전하지만 엄격하고, 동적 타입은 유연하고 코딩이 빠르지만 런타임 에러 확률이 높습니다.	3
108	3	프로그래밍에서 메모리 누수(Memory Leak)가 발생하는 원인을 설명하세요.	더 이상 참조할 필요가 없는 객체의 참조 고리를 끊지 않고 계속 유지하여 가비지 컬렉터가 해제하지 못하는 상태입니다.	3
109	3	자바스크립트의 클로저(Closure)의 개념을 설명하세요.	외부 함수가 종료된 후에도 외부 함수의 스코프(변수 환경)에 접근할 수 있는 내부 함수입니다.	3
110	3	이진 탐색(Binary Search) 알고리즘의 전제 조건과 시간복잡도를 설명하세요.	데이터 배열이 반드시 정렬되어 있어야 하며, 탐색 시 매 단계마다 범위를 반으로 줄여 O(log n)의 속도를 냅니다.	3
111	3	해시 충돌(Hash Collision)이 일어났을 때 분리 체이닝(Chaining)의 작동 원리를 설명하세요.	동일한 해시 값이 나오면 해당 버킷에 연결 리스트(Linked List) 형태로 객체들을 주렁주렁 매달아 보관합니다.	3
112	3	그리디 알고리즘(Greedy)의 핵심 아이디어를 설명하세요.	매 순간 선택지 중 최선의 선택을 해 나가는 방식으로, 항상 전체적인 최적해를 보장하지는 않습니다.	3
113	3	비트 연산(Bitwise Operation)을 활용하는 이유를 설명하세요.	컴퓨터가 가장 빠르게 처리하는 이진수 단위의 연산이므로 메모리를 절약하고 연산 속도를 극대화할 수 있기 때문입니다.	3
114	3	자바에서 제어자 `final`의 용도 3가지를 설명하세요.	클래스에 붙이면 상속 불가, 메서드에 붙이면 오버라이딩 불가, 변수에 붙이면 상수를 뜻합니다.	3
115	3	프로그래밍에서 세그먼트 트리(Segment Tree)는 언제 사용되나요?	배열의 특정 구간 합이나 최솟값 등을 구하는 쿼리를 복잡도 O(log n)으로 빠르게 처리해야 할 때 사용합니다.	3
116	3	정렬 중 안정 정렬(Stable Sort)의 정의를 설명하세요.	정렬 기준이 같은 데이터가 있을 때, 정렬 전의 기존 상대적인 순서가 정렬 후에도 그대로 유지되는 정렬입니다.	3
117	3	최단 경로를 구하는 다익스트라(Dijkstra) 알고리즘의 원리를 설명하세요.	시작 노드에서 다른 노드까지의 최단 거리를 무한대로 두고, 인접한 노드 중 가장 가까운 곳을 방문하며 최단 거리를 갱신합니다.	3
118	3	자바의 직렬화(Serialization)와 역직렬화의 개념을 설명하세요.	자바 객체를 메모리/네트워크 전송 가능한 바이너리 형태로 변환하는 것을 직렬화, 이를 다시 객체로 조립하는 것을 역직렬화라 합니다.	3
119	3	동기(Synchronous)와 비동기(Asynchronous) 호출의 차이를 설명하세요.	동기는 호출한 함수의 작업이 끝날 때까지 기다렸다가 진행하고, 비동기는 작업을 맡긴 채 즉시 다음 코드를 실행합니다.	3
121	4	프로세스와 스레드의 차이를 설명해주세요.	독립된 메모리 영역을 가진 프로세스와 자원을 공유하는 스레드의 차이입니다.	3
122	4	교착상태(Deadlock)의 발생 조건 4가지를 설명해주세요.	상호 배제, 점유 대기, 비선점, 순환 대기 조건을 갖추어야 합니다.	3
123	4	페이징(Paging)과 세그멘테이션(Segmentation)의 차이는 무엇인가요?	고정 크기 분할인 페이징과 가변 논리 단위 분할인 세그멘테이션의 차이입니다.	3
124	4	CPU 스케줄링 알고리즘의 종류를 설명해주세요.	FCFS, SJF, RR 등 다양한 선점/비선점 스케줄링이 존재합니다.	3
125	4	컨텍스트 스위칭(Context Switching)이란 무엇인가요?	CPU 사용권이 넘어갈 때 PCB에 프로세스 상태를 저장하고 복구하는 작업입니다.	3
126	4	가상 메모리(Virtual Memory)의 개념을 설명해주세요.	물리 메모리 한계를 극복하기 위해 요구 페이징 기법 등을 사용해 보조기억장치를 활용하는 주소 기술입니다.	3
127	4	인터럽트(Interrupt)와 시스템 콜(System Call)의 차이는 무엇인가요?	하드웨어적 신호인 인터럽트와 커널 권한을 요청하는 소프트웨어적인 시스템 콜의 차이입니다.	3
128	4	캐시 메모리의 지역성 원리를 설명해주세요.	시간 지역성과 공간 지역성을 바탕으로 캐시 적중률을 끌어올리는 원리입니다.	3
129	4	뮤텍스(Mutex)와 세마포어(Semaphore)의 차이를 설명해주세요.	바이너리 형태로 락을 제어하는 뮤텍스와 공유 자원 카운팅을 제어하는 세마포어의 차이입니다.	3
130	4	프로세스 동기화가 필요한 이유를 설명해주세요.	임계 영역에서 공유 자원 동시 접근 시 발생하는 경쟁 상태를 막고 데이터 일관성을 유지하기 위함입니다.	3
131	4	대칭키 암호화와 공개키(비대칭키) 암호화의 차이점을 설명하세요.	암복호화에 같은 키를 사용해 빠른 대칭키와, 서로 다른 키를 사용해 안전한 공개키의 차이입니다.	3
132	4	OSI 7계층 모델의 구조와 데이터 단위(PDU)를 설명하세요.	물리부터 응용까지 7개 계층이 있으며 비트, 프레임, 패킷, 세그먼트 등의 단위로 통신합니다.	3
133	4	TCP와 UDP의 결정적인 특성 차이를 설명하세요.	흐름 제어가 있고 연결을 확립하는 신뢰성의 TCP와, 오버헤드가 적고 빠른 비연결성 UDP의 차이입니다.	3
134	4	HTTP와 HTTPS의 보안 매커니즘 차이를 설명하세요.	HTTPS는 443 포트를 쓰며 SSL/TLS 핸드셰이크를 통해 데이터를 암호화 전송합니다.	3
135	4	DNS(Domain Name System)의 질의 프로세스를 설명하세요.	로컬 캐시 확인 후, 리커시브 서버가 루트 네임서버부터 계층적으로 찾아 IP를 반환합니다.	3
136	4	쿠키와 세션의 보안적 관점 차이를 설명하세요.	쿠키는 변조 위험이 있는 로컬 브라우저에 저장되고, 세션은 안전하게 서버 메모리에 보관됩니다.	3
137	4	CORS 정책이 발생하는 원인과 해결책을 말해주세요.	브라우저가 보안을 위해 교차 출처의 자원 요청을 차단하는 것으로, 서버 헤더 허용 설정이 필요합니다.	3
138	4	로드 밸런서(Load Balancer) 스케줄링 알고리즘 종류를 설명하세요.	라운드 로빈, 최소 연결 방식(Least Connection), 해시 방식 등이 있습니다.	3
139	4	웹소켓(WebSocket) 프로토콜의 장점을 설명하세요.	HTTP와 달리 한 번 연결되면 양방향으로 데이터를 헤더 오버헤드 없이 실시간 스트리밍할 수 있습니다.	3
140	4	DDoS(분산 서비스 거부 공격)의 개념을 설명하세요.	수많은 좀비 PC 네트워크를 이용해 타겟 서버에 과도한 트래픽을 발생시켜 마비시키는 공격입니다.	3
141	4	SQL Injection 공격 원리와 방어책을 설명하세요.	입력창에 악의적인 SQL 구문을 주입해 DB를 조작하는 공격으로, PreparedStatement를 써서 방어합니다.	3
142	4	XSS(Cross-Site Scripting) 공격이란 무엇인가요?	게시판 등에 악의적인 자바스크립트 코드를 삽입하여 다른 사용자의 쿠키나 세션 토큰을 탈취하는 공격입니다.	3
143	4	방화벽(Firewall)과 IDS/IPS의 차이를 설명하세요.	방화벽은 IP/포트 기준으로 접근을 차단하고, IDS는 침입을 탐지하며, IPS는 실시간으로 침입을 방어합니다.	3
144	4	대칭키 암호화 알고리즘 중 AES의 특징을 설명하세요.	DES의 보안 취약점을 대체하기 위해 만들어진 블록 암호화 표준으로, 현재 가장 널리 쓰이는 강력한 알고리즘입니다.	3
145	4	공개키 암호화 알고리즘 중 RSA의 원리를 설명하세요.	매우 큰 소수의 소인수분해가 어렵다는 수학적 복잡성에 기반을 둔 비대칭키 암호화 알고리즘입니다.	3
146	4	디지털 서명(Digital Signature)의 목적 2가지를 설명하세요.	송신자의 신원을 증명하는 기밀성과, 메시지가 도중에 변조되지 않았음을 증명하는 부인 방지입니다.	3
147	4	OAuth 2.0 프로토콜의 핵심 개념을 설명하세요.	비밀번호를 직접 제공하지 않고 권한 토큰(Access Token)을 발급받아 서드파티 앱에 인증을 위임하는 표준입니다.	3
148	4	Docker와 같은 컨테이너 기반 가상화의 장점을 설명하세요.	게스트 OS 하이퍼바이저 없이 호스트 커널을 공유하므로 경량화되어 가볍고 배포 속도가 매우 빠릅니다.	3
149	4	쿠버네티스(Kubernetes)의 주된 역할을 설명하세요.	도커 컨테이너들의 배포, 스케일링, 로드밸런싱, 자동 복구(Self-healing)를 자동화해 주는 오케스트레이션 툴입니다.	3
150	4	클라우드 컴퓨팅 서비스 모델인 IaaS, PaaS, SaaS의 차이를 설명하세요.	IaaS는 가상 인프라 제공, PaaS는 개발 플랫폼 환경 제공, SaaS는 완성된 소프트웨어 서비스를 제공합니다.	3
151	4	서버 가상화에서 하이퍼바이저(Hypervisor)의 역할을 설명하세요.	단일 물리 서버 하드웨어 위에 여러 개의 독립적인 가상머신(VM)을 띄우고 자원을 분배 제어합니다.	3
152	4	네트워크 장비 중 라우터(Router)의 핵심 기능을 설명하세요.	서로 다른 네트워크를 연결하고 패킷의 최적 경로를 지정해 주는 라우팅 기능을 수행합니다.	3
153	4	서브네팅(Subnetting)을 하는 목적을 설명하세요.	하나의 거대한 네트워크를 여러 개의 작은 네트워크로 쪼개어 IP 주소 낭비를 막고 브로드캐스트 부하를 줄입니다.	3
154	4	DHCP 프로토콜의 역할을 설명하세요.	네트워크에 접속한 단말기에게 IP 주소, 서브넷 마스크, 게이트웨이 주소를 자동으로 할당해 주는 프로토콜입니다.	3
155	4	RAID(레이드) 구성 중 RAID 0과 RAID 1의 차이를 설명하세요.	RAID 0은 데이터를 분산 저장(스트라이핑)하여 속도가 빠르고, RAID 1은 똑같이 복사(미러링)하여 안정성이 높습니다.	3
156	4	정보보호 3대 요소인 CIA를 설명하세요.	인가된 유저만 접근하는 기밀성, 데이터가 정확해야 하는 무결성, 필요할 때 쓰는 가용성입니다.	3
157	4	포트 포워딩(Port Forwarding)이란 무엇인가요?	외부 네트워크에서 공유기의 특정 포트로 들어온 요청을 내부망에 있는 특정 기기의 포트로 토스해 주는 기술입니다.	3
158	4	서버 모니터링에서 CPU 사용량 중 어떤 지표가 중요하나요?	전체 CPU 사용량 외에도 I/O Wait(디스크 대기 시간) 비율을 확인하여 병목 현상 원인을 분석해야 합니다.	3
159	4	프로젝트 관리에서 일정 지연을 막기 위한 주공정법(CPM)의 개념을 설명하세요.	전체 프로젝트 기간을 결정하는 가장 긴 작업 경로인 임계 경로(Critical Path)를 찾아 집중 관리하는 기법입니다.	3
160	4	세션 기반 인증과 JWT(JSON Web Token) 토큰 인증의 큰 차이를 설명하세요.	세션은 서버 메모리에 상태를 저장하지만, JWT는 토큰 자체에 정보를 담아 클라이언트가 들고 다니므로 무상태 서버 확장에 유리합니다.	3
20	1	REST 아키텍처 스타일의 주요 제약 조건을 말해주세요.	무상태성, 클라이언트-서버 구조, 캐시 가능성, 일관된 인터페이스 등이 있습니다.	3
76	2	낙관적 락(Optimistic Lock)과 비관적 락(Pessimistic Lock)의 차이를 설명하세요.	비관적 락은 실제 데이터에 락을 걸고, 낙관적 락은 버전 체크를 통해 충돌을 방지합니다.	3
120	3	자바의 싱글프레임워크 스레드와 멀티스레드의 실행 스케줄링 방식을 설명하세요.	자바 스레드는 JVM 스케줄러에 의해 우선순위(Priority) 방식과 라운드 로빈(Round-Robin) 방식이 결합된 스레드 스케줄링에 따라 시분할 제어됩니다.	3
\.


--
-- TOC entry 4950 (class 0 OID 16388)
-- Dependencies: 220
-- Data for Name: subjects; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.subjects (id, name) FROM stdin;
1	소프트웨어 설계/개발
2	DB구축
3	프로그래밍 언어 활용
4	정보시스템 구축 관리
\.


--
-- TOC entry 4956 (class 0 OID 16483)
-- Dependencies: 226
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, password_hash, name, created_at) FROM stdin;
1	gildong123	hashed_password_sample	홍길동	2026-05-17 16:07:51.979105
\.


--
-- Data for Name: interview_session; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.interview_session (id, session_id, user_id, subject, avg_score, overall_feedback, avg_duration, status, created_at) VALUES
(1, 'sample-session-uuid-1', 1, '소프트웨어 설계/개발', 96, '핵심 제약 조건인 무상태성과 일관된 인터페이스를 정확히 짚었습니다.', 30, 'COMPLETED', '2026-05-17 16:07:51.979105');

--
-- Data for Name: answer_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.answer_log (id, question_id, user_answer, ai_feedback, score, session_id, missing_keywords, matched_keywords, captured_image_path, duration, created_at) VALUES
(1, 20, 'REST 아키텍처는 무상태성이 중요하고 일관된 인터페이스를 가집니다.', '핵심 제약 조건인 무상태성과 일관된 인터페이스를 정확히 짚었습니다.', 96, 1, NULL, '무상태성, 일관된 인터페이스', '/uploads/test_user1.png', 30, '2026-05-17 16:07:51.979105');


--
-- TOC entry 4969 (class 0 OID 0)
-- Dependencies: 223
-- Name: question_keywords_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.question_keywords_id_seq', 475, true);


--
-- TOC entry 4970 (class 0 OID 0)
-- Dependencies: 221
-- Name: questions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.questions_id_seq', 160, true);


--
-- TOC entry 4971 (class 0 OID 0)
-- Dependencies: 219
-- Name: subjects_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.subjects_id_seq', 4, true);


--
-- TOC entry 4972 (class 0 OID 0)
-- Dependencies: 225
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- TOC entry 4973 (class 0 OID 0)
-- Dependencies: 227
-- Name: voice_learning_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.interview_session_id_seq', 1, true);
SELECT pg_catalog.setval('public.answer_log_id_seq', 1, true);
SELECT pg_catalog.setval('public.review_state_id_seq', 1, false);


--
-- TOC entry 4791 (class 2606 OID 16426)
-- Name: question_keywords question_keywords_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.question_keywords
    ADD CONSTRAINT question_keywords_pkey PRIMARY KEY (id);


--
-- TOC entry 4789 (class 2606 OID 16411)
-- Name: questions questions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);


--
-- TOC entry 4785 (class 2606 OID 16543)
-- Name: subjects subjects_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_name_key UNIQUE (name);


--
-- TOC entry 4787 (class 2606 OID 16396)
-- Name: subjects subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_pkey PRIMARY KEY (id);


--
-- TOC entry 4793 (class 2606 OID 16493)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 4795 (class 2606 OID 16495)
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: interview_session interview_session_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.interview_session
    ADD CONSTRAINT interview_session_pkey PRIMARY KEY (id);

--
-- Name: interview_session interview_session_session_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.interview_session
    ADD CONSTRAINT interview_session_session_id_key UNIQUE (session_id);

--
-- Name: answer_log answer_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.answer_log
    ADD CONSTRAINT answer_log_pkey PRIMARY KEY (id);

--
-- Name: review_state review_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.review_state
    ADD CONSTRAINT review_state_pkey PRIMARY KEY (id);

--
-- Name: review_state uk_review_state_user_question; Type: CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.review_state
    ADD CONSTRAINT uk_review_state_user_question UNIQUE (user_id, question_id);

--
-- Name: interview_session fk_session_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.interview_session
    ADD CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: answer_log fk_log_question; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.answer_log
    ADD CONSTRAINT fk_log_question FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;

--
-- Name: answer_log fk_log_session; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.answer_log
    ADD CONSTRAINT fk_log_session FOREIGN KEY (session_id) REFERENCES public.interview_session(id) ON DELETE CASCADE;

--
-- Name: review_state fk_review_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.review_state
    ADD CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

--
-- Name: review_state fk_review_question; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--
ALTER TABLE ONLY public.review_state
    ADD CONSTRAINT fk_review_question FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- TOC entry 4799 (class 2606 OID 16427)
-- Name: question_keywords question_keywords_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.question_keywords
    ADD CONSTRAINT question_keywords_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- TOC entry 4798 (class 2606 OID 16412)
-- Name: questions questions_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_subject_id_fkey FOREIGN KEY (subject_id) REFERENCES public.subjects(id) ON DELETE CASCADE;


-- Completed on 2026-05-17 16:09:58

--
-- PostgreSQL database dump complete
--

\unrestrict Eznf0CTH93Uei6ldvHHwp91AwB7fwg8Ua7QT5phuEdC5bXMJPn7js6zqQwkSKpD

