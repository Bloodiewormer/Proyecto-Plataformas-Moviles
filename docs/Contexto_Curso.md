# NotaViva — EIF411 Course Context

> **Purpose:** Neutral input document. Records the fixed framework in which the project is developed — calendar, mandatory deliverables, required stack, and available time — plus the NotaViva brief exactly as it was proposed and selected.
>
> It does not contain the team's design, architecture, or scope decisions. Those live in the project's technical documentation, which is maintained separately.
>
> **Last update:** August 16, 2026 — Document translated to English (per D-14), backend stack updated to Kotlin (D-13), serialization locked to Gson (D-19).

---

## 1. Course Identification

| Field | Value |
|---|---|
| Course | Mobile Platform Design and Programming |
| Code | EIF 411 |
| Major | Information Systems Engineering (ISIN) — Bachelor's |
| University | Universidad Nacional (UNA), School of Informatics |
| Level | Level III, Bachelor's |
| Term | II Term 2026 |
| Modality | Remote in-person |
| Nature | Theoretical-practical |
| Credits | 4 |
| Weekly hours | 11 (2 theory · 2 lab · 7 independent study) |
| Session | Wednesday 18:00 – 21:20 |
| Consultation | Tuesday 17:00 – 19:00 |
| Professor | Maikol Guzmán Alán — maikol.guzman.alan@una.cr |
| Prerequisite | EIF209 Programming IV |
| Co-requisite | N/A |

**Team:** X-Ray — **Brandon Brenes, David González, and Felipe Ugalde (3 members)**.

> **Correction:** A previous version of this document indicated a team of 2 and a cap of "maximum 2 people" per group. This is incorrect. The team consists of **3 members for all grading rubrics, including the scientific article**. The nine groups in the cohort consist of 3 or 4 people, according to the professor's roll call during the August 12 session.

---

## 2. Evaluation

| Rubric | Value |
|---|---|
| In-class labs | 60 % |
| Scientific article (in English, group — X-Ray does it among the 3) | 10 % |
| Final project | 30 % |
| **Total** | **100 %** |

- The final project is group work with **individual grading**, based on each student's contribution.
- Minimum passing grade: **70 %**.
- There is no extraordinary makeup exam; the final grade is the direct sum of the rubrics.

### Delivery Rules

1. Any deliverable not presented on time, that does not compile, or lacks minimal functionality receives a **0**.
2. **Minimal functionality = at least 40%** of the requested functionality, working at runtime.
3. Plagiarism in code, documentation, or algorithms triggers UNA's internal regulations.
4. **Labs are NOT uploaded to any platform.** They are reviewed live, in class, in defense mode. The professor was explicit: *"I don't review anything in the virtual classroom, nothing, absolutely nothing."* The virtual classroom is only used to publish grades.
5. The **code IS uploaded to GitHub Classroom**.
6. **Correction cycle:** Lab observations are corrected and presented live again the following week, at the end of the class. The professor returns 100% of the points if the correction is complete: *"if you show it to me, I return 100% of the grade."* If the correction is not presented, the original grade stands.

### Practical Consequence on Dates

Since everything is defended in class and the session is **Wednesday 18:00–21:20**, the operative date for each lab is **the last Wednesday class before the deadline**, not the calendar deadline. See §4.1.

---

## 3. Program Content

### General Objective
Introduce the student to the topic of mobile devices, covering concepts, architecture, features, categories, and the scope of the paradigm.

### Specific Objectives
1. Concepts, architectures, and categories of mobile devices.
2. GUI elements and animation.
3. Distributed programming using sockets and threads.
4. Storage in mobile devices as a persistent system.
5. Emulatior visualization and device execution.
6. Analysis of dominant mobile technologies in the market.
7. Integration of AI services (LLMs, agents, and protocols like MCP) as functional components in native mobile applications.

### Modules

| Module | Weeks | Topics |
|---|---|---|
| **1. Design Patterns** | 2 | MVC, MVP, MVVM, Singleton, Observer, Iterator · Kotlin fundamentals |
| **2. Intro to Mobile Platforms** | 2 | iOS/Android/Windows Phone architectures · History of smartphones & tablets · Frameworks · Configurations & profiles · Emulators (min. 2) · APK generation |
| **3. GUI & AI-Assisted Prototyping** | 4 | Paper prototyping and mockups · XML Layouts (legacy) and Jetpack Compose · Activities, Widgets, Events · Usability, i18n, accessibility, styling |
| **4. Distributed Programming & Networks** | 3 | Async tasks · Inter-process communication · Sockets, HTTP/HTTPS, push, XML/JSON, NFC, Bluetooth, WiFi · Media (camera, audio, video, GPS) · Front-end ↔ back-end |
| **5. Storage Types** | 2 | Local files (binary and text) · Local and remote databases · Cloud storage |
| **6. Current Topics** | 2 | Security (accounts, permissions, signing, roles, sensitive data) · Performance (memory, battery, leaks, logs) · Store publishing · Generative AI integration |

### Declared Transversal Axis

The program incorporates generative AI **as a structural component** of the developed apps, including LLM integration and interoperability protocols like **MCP (Model Context Protocol)**.

Submodule 6.4:
- Generative AI embedded in apps (assistants, content generation, recommendation).
- LLM API consumption (OpenAI, Claude, Gemini) from Android using Kotlin.
- MCP: concept and use cases to connect apps with external tools.
- Cost, privacy, and sensitive data considerations when integrating AI.

### Methodological Strategy
1. Group labs — X-Ray works on them among the 3 members.
2. Scientific article in English, group — all 3 members.
3. Final mobile development project.

Each of the 15 sessions includes a lab, a research task, and a short evaluation.

---

## 4. Deliverables Calendar

### 4.1 Master Table

| # | Deliverable | Open | Official Close | **Real Defense (Wed)** | Status |
|---|---|---|---|---|---|
| Lab 1 | Wireframes (Figma) + ERD/Class diagram | Wed Jul 29 | Thu Aug 13 | **Defended Wed Aug 12** | **85** — correction **Wed Aug 19** |
| Lab 2 | Android/Kotlin Activities | Sat Aug 8 | Sat Aug 29 | **Wed Aug 26** | Open |
| Lab 3 | Error handling, Hilt, Retrofit, mock API | Wed Aug 26 | Sun Sep 13 | **Wed Sep 9** | — |
| Lab 4 | Backend fundamentals & data persistence | Wed Sep 9 | Sat Sep 26 | **Wed Sep 23** | — |
| Lab 5 | Services, Web API, DTO & testing | Wed Sep 23 | Sat Oct 17 | **Wed Oct 14** | — |
| Lab 6 | Security, AI & Cloud | Wed Sep 30 | Sat Oct 31 | **Wed Oct 28** | — |
| — | Applied Research | Wed Nov 11 | Sun Nov 15 | **Wed Nov 11** | — |
| — | Final Project | Wed Nov 11 | Sun Nov 15 | **Wed Nov 11** | — |

> The "Real Defense" column advances each delivery between 1 and 4 days regarding the official closing date. The tightest case is **Lab 2: 2 effective weeks, not 3**, with one of them shared with the Lab 1 correction (Aug 19).

### 4.2 Overlaps

```text
AUG        |13|    |20|    |27|      SEP |03| |10| |17| |24|   OCT |01| |08| |15| |22| |29|  NOV |05| |12|15
Lab 1  ====X
Lab 2      ===============X
Lab 3              ==================X
Lab 4                        =================X
Lab 5                                  =========================X
Lab 6                                      =================================X
Inv/Final                                                                             ==X
```

- **Aug 26 – Sep 13:** Lab 2 (closing) and Lab 3 open.
- **Sep 23 – Sep 26:** Labs 4, 5, and 6 open simultaneously.
- **Nov 1 – Nov 10:** Only period of the semester with no open deliverables.

### 4.3 Available Time

- From August 13 to November 15: **~13.5 weeks**.
- Independent study assigned by the program: 7 h/week per person → **~95 h per person**, **~285 h per team** (3 members) in the semester.
- This total includes the scientific article, research tasks, and short evaluations for each session, not just development.
- Capacity warning: 3 people do not yield 1.5× the output of 2. Coordination has a cost, and during overlap weeks (see §4.2), effective capacity drops.

---

## 5. Lab Details

### Lab 1 — App Wireframes
**Jul 29 – Aug 13 · defended Aug 12 · score 85**

Objective: Develop the application prototype using **Figma** and the **entity-relationship/class diagram (database)**.

**Obtained result — single deduction:**

| Rubric | Score | Descriptor |
|---|---|---|
| DB Objects UML Diagram | **1.5 / 3** | *Diagram present but with incomplete relationships or entities* |

Professor's observations on that rubric:
- **You need the `privilege`, `roles` tables.**
- **Explore the use of JSON.**

All other rubrics received maximum points. The observation the professor made at the end of the session about the second user role (*"I missed seeing the different role in the X-Ray group... you almost lose 50% of the course"*) **was not a deduction from Lab 1**: it is a forward-looking requirement impacting Lab 2, where login, registration, menu, and navigation must exist for two distinct roles.

**Correction to be presented on Wednesday, August 19** to recover the 15 points.

### Lab 2 — Activities in Android
**Aug 8 – Aug 29**

Objective: Develop the most important screens defined in the prototype in Android with Kotlin. Elements the brief demands to be covered:
- Menu
- Login
- Register
- Main
- Navigation
- Lists

### Lab 3 — Error handling, Hilt, Retrofit, and mock API consumption
**Aug 26 – Sep 13**

Objective: Evaluate the implementation of a modern Android architecture through:
- Dependency injection with **Hilt**.
- Web service consumption with **Retrofit** (GET, POST as appropriate).
- Automatic deserialization with **Gson** (Fixed per D-19).
- Best practices in error handling during service calls.
- Integration with a **simulated API (mock)** to test real data consumption scenarios.

### Lab 4 — Backend fundamentals and data persistence
**Sep 9 – Sep 26**

Objective: Ensure understanding of base backend and persistence concepts. Topics:
- Intro to backend.
- Distributed programming (concepts and basic examples).
- Data layer: database design (relational modeling, primary and foreign keys).
- Relationships between entities.
- **Repository Pattern.**

### Lab 5 — Services, Web API, DTO, and testing
**Sep 23 – Oct 17**

Objective: Validate the implementation of business logic and service exposure via a REST API. Topics:
- Service layer.
- WebAPI layer (REST controllers).
- Input and output DTOs.
- Testing with **Postman** (endpoints, headers, status codes).

### Lab 6 — Security, Artificial Intelligence, and Cloud
**Sep 30 – Oct 31**

Objective: Evaluate advanced aspects such as security, external service integration, and cloud deployment. Topics:
- **Security with JWT using Spring Security.**
- **Integration with external APIs** (ChatGPT, Claude, Gemini, etc.).
- **Cloud deployment** (Render, Railway, Heroku, AWS).

### Applied Research
**Nov 11 – Nov 15**

Objective: Integrate an additional topic into the project, selected at the start of the course, to deepen complementary software development aspects. Evaluation is based on the **creativity and effectiveness** of applying the researched knowledge.

Presentation requirements:
- Clear, concise intro to the topic, understandable for unfamiliar peers.
- **Mandatory Q&A round:** Every non-presenting group must ask at least one technical or critical question. A rotating spokesperson can be designated. Evaluated on relevance and depth.

**Topic assigned to X-Ray:** *Local storage implementation and data synchronization.*

### Final Project
**Nov 11 – Nov 15**

Objective: Culminate prototype development by integrating all course labs. Delivery must include:
1. Final app version distributed via **Firebase App Distribution**.
2. Login screens integrated with **JWT** security and connected to the **backend API**.
3. All necessary adjustments to ensure correct and stable operation.
4. **Satisfaction survey for at least 5 external people**, considering:
   - General usability and functionality.
   - Improvement recommendations.
   - Bugs or errors detected during the test.

---

## 6. Required Stack

Each element is evaluated in a specific lab. It is the mandatory minimum set, regardless of the team's own technical decisions.

| Layer | Technology | Required In |
|---|---|---|
| Language / IDE | Kotlin, Android Studio | Full course |
| Architecture | MVVM | Module 1 |
| UI | Jetpack Compose (or legacy XML) | Module 3 / Lab 2 |
| Prototyping | Figma | Lab 1 |
| Dependency Injection | Hilt | Lab 3 |
| HTTP Client | Retrofit + **Gson (D-19)** | Lab 3 |
| Local Persistence | Local database (Room) | Module 5 / App. Research |
| Backend | Spring Boot on **Kotlin (D-13)** | Labs 4–6 |
| Data Patterns | Repository, DTOs | Labs 4–5 |
| Security | JWT + Spring Security | Lab 6 / Final |
| Deployment | Render, Railway, Heroku or AWS | Lab 6 |
| Distribution | Firebase App Distribution | Final |
| API Testing | Postman | Lab 5 |
| Generative AI | External LLM API | Lab 6 / Module 6 |
| Emulators | At least 2 configured | Module 2 |

---

## 7. Project Brief: NotaViva

*Source: Cohort's How–Now–Wow matrix, closed July 29, 2026.*

### 7.1 Definition

**NotaViva** — Original proposal from Group Charlie (Keneth Jara Herrera), pitched with GPT-4o-mini (vision + text). It was **selected as the course's guiding application**, meaning all nine groups develop variants of it.

> The student photographs their notes, the AI reconstructs them as structured text, audits them against the official course syllabus, and generates quizzes covering detected gaps. The cycle closes: **organize → audit → reinforce**.

### 7.2 Score Obtained

Matrix weighting: Core AI 35% · Viability 30% · Innovation 20% · Course Coverage 15%.

| Axis | NotaViva |
|---|---|
| Core AI | 9.5 |
| Viability | 8.0 |
| Innovation | 8.5 |
| Course Coverage | 9.5 |
| **Total** | **8.83 — 1st out of 9, WOW quadrant** |

### 7.3 Selection Rationale

It is the only proposal that covers the five technical axes of the course without forcing them:
- Camera as an indispensable input, not decorative.
- Authentication.
- Real CRUD.
- Three roles.
- Push notifications.
- Three distinct AI calls: **vision, semantic comparison, and generation**.

Pedagogical advantage: The use case is the course itself — you can load the EIF411 syllabus and test the app against real class materials.

### 7.4 Identified Risk in the Brief

> **Risk:** OCR on poor-quality handwriting is the fragile point.
>
> **Proposed Mitigation in original brief:** A review screen where the student corrects the transcription before the AI compares it. Reduces OCR dependency without breaking the flow.

### 7.5 Cohort Signals Applicable to the Project

| Signal | Content |
|---|---|
| **Scope collision** | NotaViva (Charlie) and cUNA (Bravo) propose the same mechanism: comparing student notes against official material to detect gaps. The observation recommends assigning distinct territories before starting. |
| **API key on the client** | Applies to all nine proposals: the model key cannot live in the APK. It flags the need to define a minimal intermediary backend before the first model call. |
| **Cohort level** | No team proposed a chatbot bolted on at the end; 6 of 9 proposals describe AI operating inside the flow, with defined input and output. |
| **Sensor coverage** | Three cohort proposals ended up with no native capabilities. NotaViva is not among them: the camera is a mandatory input to the flow. |

### 7.6 Note on Team X-Ray

X-Ray's original proposal was **Simula** (David González): an AI conversational trainer. It scored the cohort's highest in Core AI (10.0) but lowest in Course Coverage (4.0) because it was exclusively text-based, requiring no camera, GPS, or permissions.
By developing NotaViva, the team works on a brief with a 9.5 course coverage.

---

## 8. Consolidated Mandatory Requirements

Checklist derived strictly from official briefs, excluding internal design choices:

- [ ] Wireframes in Figma
- [ ] Entity-relationship/class diagram
- [ ] Menu, login, register, main, navigation, and lists in Kotlin
- [ ] Hilt (Dependency Injection)
- [ ] Retrofit + automatic deserialization (Gson)
- [ ] Error handling in service calls
- [ ] Mock API consumption
- [ ] Database with relational modeling and relationships
- [ ] Repository Pattern
- [ ] Service layer
- [ ] REST controllers with input and output DTOs
- [ ] Endpoint testing with Postman
- [ ] JWT + Spring Security
- [ ] External LLM API integration
- [ ] Backend deployed to the cloud
- [ ] At least 2 configured emulators
- [ ] APK distributed via Firebase App Distribution
- [ ] Satisfaction survey (5 external people)
- [ ] Applied research presented, with Q&A round
- [ ] Scientific article in English

### 8.1 Transversal Requirements Dictated in Class (Aug 12 Session)

Applicable to all nine groups, not found in written briefs:

- [ ] **Minimum 2 user roles** — mandatory. The NotaViva brief actually outlines 3.
- [ ] **`users`, `roles`, `privileges` tables + `user_roles`, `role_privileges` bridges**, matching the professor's dictated schema.
- [ ] **`users` in plural** — `user` is a reserved word in Postgres.
- [ ] **All entity names must be in English**.
- [ ] **PostgreSQL is mandatory** as the main DB.
- [ ] **Navigation menu is mandatory** in the app.
- [ ] **If notifications are implemented, they must be real push notifications**, not simulated.
- [ ] **If offline mode is implemented, it must be real**, with a local DB; otherwise, explicitly declare it as a future feature.
- [ ] **Authentication matching the framework standard** (Spring Security).
- [ ] **Application name must be distinct** from other groups.
- [ ] **Custom color palette** — professor explicitly advised against default AI "purple".
- [ ] **Evaluate JSON/JSONB fields** for quizzes, questions, options, and flashcards, rather than fully normalizing everything.
- [ ] **Reduce data model size** — *"very large databases become hyper-complex to manage."*
- [ ] **Use `SharedPreferences`** for app settings, not a settings table.
- [ ] **Document the AI dialogue exercise** used for the data model.
- [ ] **Monolithic architecture** at this stage; serverless elements come later.
- [ ] Persistence can be **fragmented**: main is relational, but a specific section can use NoSQL.
- [ ] It is **explicitly permitted to take inspiration from other groups' proposals**.

---

## 9. Course Bibliography

- AlMulla, B., Assi, M., & Hassan, S. (2026). *Understanding the Challenges and Opportunities of Generative AI Apps: An Empirical Study.* arXiv:2506.16453
- Anthropic (2024). *Introducing the Model Context Protocol.*
- Model Context Protocol (2024). Official specification and technical documentation.
- McWherter, J., & Gowell, S. (2012). *Professional mobile application development.* John Wiley & Sons.
- Miguel, R. M. (2012). *Desarrollo de aplicaciones para Android.* Grupo Editorial RA-MA.
- Robledo, D. (2016). *Desarrollo de aplicaciones para Android I.* Ministerio de Educación.
- Lecheta, R. R. (2017). *Android Essencial con Kotlin.* Novatec Editora.
- Fonseca Camargo, Y., Pertuz Toscano, K., & Martelo López, E. (2018). *Aplicaciones nativas vs. aplicaciones híbridas en el desarrollo de aplicaciones móviles para Android e iOS.*
- Sánchez Rueda, F. (2020). *Comparativa Kotlin y Java en desarrollo Android.*