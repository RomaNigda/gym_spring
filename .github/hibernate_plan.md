# План виконання завдання Hibernate (Gym CRM)

Припущення: завдання Spring Core вже виконано (сервіси, фасад, генерація username/password, тести, логування).

## Етап 1. Підготовка проєкту та БД
1. Додати залежності: `hibernate-core`, драйвер СУБД (PostgreSQL), `HikariCP`, `spring-orm`, `spring-tx`.
2. СУБД - PostgreSQL.
3. Винести налаштування в `application.properties`: url, user, password, dialect, `hibernate.hbm2ddl.auto`, `show_sql`.
4. Налаштувати `LocalSessionFactoryBean` та `HibernateTransactionManager` у Java-конфігурації, додати `@EnableTransactionManagement`.
5. Підготувати SQL-скрипт для довідника `training_type` (константний, з застосунку не змінюється).

## Етап 2. Entity-моделі за схемою
1. **User** (батьківська сутність): `id`, `firstName`, `lastName`, `username`, `password`, `isActive`; успадкування `JOINED`.
2. **Trainee**: успадковує User; `id` є спільним ключем, `dateOfBirth` та `address` необов'язкові.
3. **Trainer**: успадковує User; `id` є спільним ключем, спеціалізація має зв'язок `@ManyToOne` із TrainingType.
4. **TrainingType**: `id`, `trainingTypeName`.
5. **Training**: `id`, `@ManyToOne` на Trainee, Trainer і TrainingType, а також `trainingName`, `trainingDate`, `trainingDuration`.
6. Зв'язок Trainee–Trainer many-to-many реалізується через окрему join-таблицю.
7. Видалення Trainee: каскад `REMOVE` / `orphanRemoval` на тренінги, щоб вони видалялися разом із профілем.
8. Валідація обов'язкових полів: `@Column(nullable = false)` та Bean Validation (`@NotBlank`, `@NotNull`).

## Етап 3. DAO-шар (заміна Map-сховища на Hibernate)
1. Замість in-memory Map зробити DAO на `SessionFactory`: `TraineeDao`, `TrainerDao`, `TrainingDao`, `TrainingTypeDao`.
2. Методи DAO:
   - save / update / delete
   - `findByUsername`
   - `findTrainingsByCriteria` для Trainee (fromDate, toDate, trainerName, trainingType) і для Trainer (fromDate, toDate, traineeName) через HQL з динамічними умовами
   - `findTrainersNotAssignedToTrainee(username)` (підзапит або `NOT IN` / `NOT EXISTS`)
3. Прибрати код ініціалізації Map з файлу та post-processor, якщо вони більше не потрібні.

## Етап 4. Сервісний шар (18 функцій)
Усі методи, крім створення профілів, виконуються лише після автентифікації (перевірка username + password).

| № | Функція | Де |
|---|---------|----|
| 1–2 | Create Trainer / Trainee (генерація username та password за правилами з Spring Core) | Trainer/TraineeService |
| 3–4 | Username & password matching | AuthenticationService |
| 5–6 | Select profile by username | Trainer/TraineeService |
| 7–8 | Change password | Trainer/TraineeService |
| 9–10 | Update profile | Trainer/TraineeService |
| 11–12 | Activate / Deactivate (не ідемпотентна: кожен виклик перемикає стан) | Trainer/TraineeService |
| 13 | Delete trainee by username (hard delete + каскад) | TraineeService |
| 14–15 | Trainings list за критеріями | TrainingService |
| 16 | Add training | TrainingService |
| 17 | Trainers not assigned to trainee | TrainerService |
| 18 | Update trainee's trainers list | TraineeService |

Загальні вимоги:
- `@Transactional` на методах, що змінюють дані; для читання `readOnly = true`.
- Перед create/update перевіряти обов'язкові поля.
- Для username з однаковими ім'ям і прізвищем додавати серійний номер, перевіряючи через БД.
- Логування (SLF4J) на ключових діях: створення, автентифікація, помилки.

## Етап 5. Автентифікація
1. `AuthenticationService` з методом `authenticate(username, password)`, який кидає власний виняток, якщо дані не збігаються.
2. Викликати його фасаді перед викликом сервісу.

## Етап 6. Фасад / точка входу
1. Оновити `GymFacade`, додавши 18 операцій. Сервіси інжектуються через конструктор.

## Етап 7. Тести
1. Unit-тести сервісів: JUnit 5 + Mockito (мок DAO).
3. Покрити:
   - успішну й неуспішну автентифікацію
   - генерацію username з дублікатами
   - валідацію
   - перемикання active
   - каскадне видалення
   - фільтри списку тренінгів

## Етап 8. Відповідь на пункт 14 (Training і TrainingType окремо)
Коротко описати в README:
- довідник типів не дублюється в кожному тренінгу (нормалізація);
- назва типу змінюється в одному місці;
- цілісність даних через FK, типи можна використовувати для спеціалізації тренера;
- простіше фільтрувати за типом.

## Рекомендований порядок роботи
1. Конфігурація Hibernate та БД
2. Entity
3. DAO
4. Автентифікація та створення профілів (1–4)
5. Select, зміна пароля, update, activate (5–12)
6. Delete та тренінги (13–18)
7. Тести та логування паралельно
8. README з відповіддю на пункт 14
