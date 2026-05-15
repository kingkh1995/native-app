## Context

椤圭洰褰撳墠浣跨敤 H2 鍐呭瓨鏁版嵁锟?(`jdbc:h2:mem:healthdb`)锛屼粎婊¤冻 Spring Boot Actuator 鍋ュ悍妫€鏌ョ殑 DataSource 瑕佹眰銆傞」鐩湰韬病鏈変笟鍔℃暟鎹寔涔呭寲闇€姹傦紝浣嗗凡锟?`spring-boot-starter-jdbc` 渚濊禆锟?
鐜版湁鍙娴嬫€у熀纭€璁炬柦锛圤Tel Collector + Prometheus + Tempo + Loki + Grafana锛夊凡閫氳繃 `compose.yaml` 绠＄悊锛宍spring-boot-docker-compose` 鍙湪 `bootRun` 鏃惰嚜鍔ㄥ惎鍔ㄨ繖浜涙湇鍔★拷?
涓変釜杩愯闃舵鐩墠鏁版嵁搴撶幆澧冧笉涓€鑷达細

| 闃舵 | 褰撳墠鏁版嵁锟?| 鐩爣鏁版嵁锟?|
|---|---|---|
| 鏈湴寮€锟?(bootRun) | H2 鍐呭瓨 | MySQL (compose.yaml) |
| 娴嬭瘯 (test) | H2 鍐呭瓨 | MySQL (TestContainers) |
| 鐢熶骇 (native image) | H2 鍐呭瓨 | MySQL (澶栭儴/Docker) |

**绾︽潫鏉′欢锟?*
- GraalVM native-image 鏋勫缓锛堥€氳繃 Paketo buildpacks锟?- 椤圭洰褰撳墠锟?shell锛堝熀纭€闀滃儚 `paketobuildpacks/builder-noble-java-tiny`锟?- 娴嬭瘯涓嶅彲渚濊禆澶栭儴鍩虹璁炬柦锛堝綋锟?`ObservabilityEndToEndTest` 浣跨敤 `@BeforeAll` 妫€鏌ュ墠缃潯浠讹紝鏁版嵁搴撴祴璇曚笉搴旀部鐢ㄦ妯″紡锟?- 椤圭洰浣跨敤 Lombok銆佽櫄鎷熺嚎锟?(Virtual Threads)

## Goals / Non-Goals

**Goals:**

- 涓夐樁娈电粺涓€浣跨敤 MySQL 8.4 LTS锛屾秷闄ょ幆澧冧笉涓€锟?- 鏈湴寮€锟?(`bootRun`) 閫氳繃 `spring-boot-docker-compose` 鑷姩绠＄悊 MySQL 鐢熷懡鍛ㄦ湡
- 娴嬭瘯闃舵閫氳繃 TestContainers 鑷姩绠＄悊 MySQL 瀹瑰櫒锛岄浂澶栭儴渚濊禆
- 鐢熶骇閮ㄧ讲 (native image) 閫氳繃鐜鍙橀噺閰嶇疆 MySQL 杩炴帴
- mysql-connector-j 锟?GraalVM native-image 涓嬪彲姝ｅ父宸ヤ綔
- 鎵€鏈夋暟鎹簮閰嶇疆鍏变韩鍚屼竴锟?`application.yml` 榛樿锟?
**Non-Goals:**

- 涓嶅紩锟?JPA / Hibernate锛堜繚锟?JDBC 灞傞潰锟?- 涓嶆秹鍙婁笟鍔℃暟鎹ā鍨嬭璁★紙椤圭洰灏氭棤鎸佷箙鍖栦笟鍔″疄浣擄級
- 涓嶆秹鍙婃暟鎹縼绉伙紙褰撳墠 H2 鏃犱笟鍔℃暟鎹級
- 涓嶄慨鏀瑰凡鏈夌殑鍙娴嬫€у熀纭€璁炬柦 compose 閰嶇疆
- 涓嶆敼鍙橀」鐩殑 dev/default profile 鏃ュ織璺緞閰嶇疆

## Decisions

### D1: 缁熶竴 DataSource 閰嶇疆锛岃€岄潪 profile 闅旂

**鍐崇瓥锟?* 涓夐樁娈靛叡锟?`application.yml` 涓殑 DataSource 榛樿閰嶇疆锛岄€氳繃涓嶅悓鏈哄埗瑕嗙洊锟?
```
鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€锟?锟? application.yml (榛樿锟?                                     锟?锟? spring.datasource.url: jdbc:mysql://localhost:3306/demo     锟?锟? spring.datasource.username: demo                            锟?锟? spring.datasource.password: demopass                        锟?鈹溾攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€锟?锟?                                                             锟?锟? 鏈湴寮€锟?            娴嬭瘯                  鐢熶骇              锟?锟? spring-boot-        @ServiceConnection    DATASOURCE_URL    锟?锟? docker-compose      锟?MySQLContainer      鐜鍙橀噺          锟?锟? 鑷姩瑕嗙洊             鑷姩瑕嗙洊               瑕嗙洊             锟?鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€锟?```

**鐞嗙敱锟?* 涓変釜闃舵閮界敤 MySQL锛岄粯璁ゅ€煎凡鍙伐浣溿€傛祴璇曞拰 compose 鍚勮嚜閫氳繃鏇撮珮浼樺厛绾х殑鏈哄埗瑕嗙洊锛屾棤闇€棰濆 profile 閰嶇疆銆傜畝鍖栭厤缃鐞嗭拷?
### D2: compose.yaml 锟?MySQL service 浣滀负鏈湴寮€鍙戝拰鐢熶骇鐨勫叡浜暟鎹簮

**鍐崇瓥锟?* 鏈湴寮€锟?(`bootRun`) 鍜岀敓锟?native 瀹瑰櫒鍏辩敤涓€锟?`compose.yaml` 涓畾涔夌殑 MySQL 瀹炰緥锟?
- 鏈湴寮€鍙戯細`spring-boot-docker-compose` 鑷姩鎷夎捣 compose 涓殑 MySQL
- 鐢熶骇閮ㄧ讲锛歚docker compose up -d` 鍚姩鍏ㄩ儴鏈嶅姟锛堝寘锟?MySQL锛夛紝native 瀹瑰櫒閫氳繃 `--network=host` 锟?compose 锟?service 渚濊禆杩炴帴

**鐞嗙敱锟?* dev 锟?default 鐨勬棩蹇楄矾寰勪笉鍚屼絾鏁版嵁搴撶浉鍚岋紝鏃犻渶涓ゅ MySQL銆俢ompose 涓凡鏈夊熀纭€璁炬柦鏈嶅姟锛屾柊锟?MySQL 鏃犻渶棰濆缂栨帓宸ュ叿锟?
### D3: TestContainers 绠＄悊娴嬭瘯锟?MySQL锛屼笌 compose 瀹屽叏鐙珛

**鍐崇瓥锟?* 娴嬭瘯闃舵浣跨敤 `@ServiceConnection` + `MySQLContainer`锛屾瘡娆℃祴璇曠被鍚姩鐙珛锟?MySQL 瀹炰緥锟?
**鐞嗙敱锟?*
- 娴嬭瘯涓庢湰鍦板紑鍙戠幆澧冨畬鍏ㄩ殧绂伙紝浜掍笉褰卞搷
- 闅忔満绔彛锛屾棤绔彛鍐茬獊
- CI/CD 涓彧闇€ Docker 瀹堟姢杩涚▼锛屼笉渚濊禆 compose 涓殑 MySQL
- 娴嬭瘯缁撴潫鍚庤嚜鍔ㄩ攢姣侊紝鏃犳暟鎹畫锟?
### D4: 浣跨敤 `spring-boot-testcontainers` starter锛岀増鏈敱 Spring Boot BOM 绠＄悊

**鍐崇瓥锟?* 寮曞叆 `org.springframework.boot:spring-boot-testcontainers`锛圫pring Boot 3.1+ 瀹樻柟 starter锛夛紝鑰岄潪鐩存帴寮曞叆 `testcontainers` 鏍稿績搴擄拷?
**鐞嗙敱锟?*
- 鐗堟湰锟?Spring Boot BOM 缁熶竴绠＄悊锛屾棤闇€鎵嬪姩鎸囧畾
- 鎻愪緵 `@ServiceConnection` 绛変究鍒╂敞锟?- 锟?Spring Boot 鐨勭敓鍛藉懆鏈熺鐞嗘洿绱у瘑闆嗘垚

### D6: DatabaseHealthIndicator 娴嬭瘯绛栫暐 锟?鍙屾祴璇曠被闅旂

**鍐崇瓥锟?* 锟?UP 锟?DOWN 涓や釜鍦烘櫙鍒涘缓鐙珛鐨勬祴璇曠被锛屼娇鐢ㄤ笉鍚岀殑 Spring 涓婁笅鏂囬厤缃拷?
- `DatabaseHealthIndicatorUpTest.java`锛氫娇锟?`@Import(TestMySqlConfiguration.class)` 鍚姩 TestContainers MySQL锛岄獙锟?`health()` 杩斿洖 UP 锟?MySQL 搴撳悕鐗堟湰
- `DatabaseHealthIndicatorDownTest.java`锛氫娇锟?`@Import(BrokenJdbcConfig.class)` + `@Primary` 鎻愪緵 Mock JdbcTemplate锛岄獙璇佹暟鎹簱涓嶅彲杈炬椂杩斿洖 DOWN 鍙婇敊璇锟?
**鐞嗙敱锟?*
- 涓や釜鍦烘櫙闇€瑕佺殑 Spring Bean 閰嶇疆涓嶅悓锛屽叡浜笂涓嬫枃浼氬鑷翠簰鐩告薄锟?- UP 鍦烘櫙闇€瑕佺湡瀹炵殑 MySQL 杩炴帴锛圱estContainers锛夛紝DOWN 鍦烘櫙闇€瑕佹ā鎷熻繛鎺ュけ锟?- Spring Boot 4.0 宸茬Щ锟?`@MockBean` / `@MockitoBean`锛屾敼锟?`@TestConfiguration` + `@Primary` 鎵嬪姩鎻愪緵 Mock Bean
- 鐙珛娴嬭瘯绫婚伩鍏嶄簡 `@DirtiesContext` 鐨勫紑閿€

**渚濇嵁 Spec 瀵瑰簲锟?*
- `specs/database-access/spec.md` 锟?"Health indicator uses MySQL" 锟?UP/DOWN 涓や釜 scenario 鍚勫搴斾竴涓祴璇曠被

### D5: mysql-connector-j GraalVM 鍏煎鎬у锟?
**鍐崇瓥锟?* 浣跨敤 Spring Boot 4.0 + GraalVM 鑷姩閰嶇疆澶勭悊 MySQL JDBC 椹卞姩锟?native-image 娉ㄥ唽锟?
**鐞嗙敱锟?* Spring Boot 4.0 锟?GraalVM 鍘熺敓鏀寔宸插寘锟?mysql-connector-j 鐨勮嚜锟?hint 娉ㄥ唽銆傛瀯寤烘椂鑻ラ亣鍒板弽灏勭己澶遍棶棰橈紝閫氳繃 `@TypeHint` 琛ュ厖銆傛椤归渶锟?`bootBuildImage` 鏃堕獙璇侊拷?
## Risks / Trade-offs

| 椋庨櫓 | 褰卞搷 | 缂撹В鎺柦 |
|---|---|---|
| mysql-connector-j 锟?native-image 涓嬬己灏戝弽灏勯厤锟?| native 鏋勫缓澶辫触鎴栬繍琛屾椂 ClassNotFoundException | Spring Boot 4.0 鐨勮嚜鍔ㄩ厤缃簲宸茶鐩栵紱鏋勫缓鏃跺锟?`--verbose` 瑙傚療锛涢锟?`@TypeHint` 鏂规 |
| MySQL 瀹瑰櫒鍚姩閫熷害褰卞搷 bootRun 棣栨鍚姩 | 棣栨鍚姩闇€鎷夐暅锟?鍒濆鍖栵紝澧炲姞 10-30s | 闀滃儚鎷夊彇涓€娆″悗缂撳瓨锛沨ealthcheck 纭繚灏辩华鍚庢墠鎺ュ彈杩炴帴 |
| TestContainers 娴嬭瘯锟?H2 娴嬭瘯锟?| 姣忎釜娴嬭瘯锟?~3-5s 瀹瑰櫒鍚姩寮€閿€ | 娴嬭瘯绫荤骇鍒殑瀹瑰櫒澶嶇敤锛坄@Testcontainers` + `@Container` static 妯″紡锛夛紱CI 涓彲鎺ュ彈 |
| `spring-boot-docker-compose` 锟?native image 鍐茬獊 | 鐢熶骇鐜涓嶅簲渚濊禆 compose | `spring-boot-docker-compose` 宸叉槸 `developmentOnly` 渚濊禆锛屼笉褰卞搷鐢熶骇鏋勫缓 |
| compose.yaml 锟?MySQL 绔彛 3306 涓庢湰鍦板凡锟?MySQL 鍐茬獊 | 绔彛琚崰鐢ㄥ锟?compose 鍚姩澶辫触 | 鐢ㄦ埛鍙慨锟?`compose.yaml` 锟?ports 鏄犲皠锛涙垨鍋滄鏈湴 MySQL |
