const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rules,
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

test("Unauthenticated user cannot read or write user profile", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.doc("users/alice").get());
  await assertFails(unauthedDb.doc("users/alice").set({
    userId: "alice",
    unitSystemWeight: "kg",
    unitSystemHeight: "cm"
  }));
});

test("Authenticated user can write and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.doc("users/alice").set({
    userId: "alice",
    unitSystemWeight: "kg",
    unitSystemHeight: "cm",
    updatedAt: Date.now()
  }));
  await assertSucceeds(aliceDb.doc("users/alice").get());
});

test("User cannot read or write another user profile", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  await assertFails(bobDb.doc("users/alice").get());
  await assertFails(bobDb.doc("users/alice").set({
    userId: "alice",
    unitSystemWeight: "kg",
    unitSystemHeight: "cm"
  }));
});

test("Authenticated user can manage their weight logs", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users/alice/weight_logs").doc("w1").set({
    id: "w1",
    userId: "alice",
    weightKg: 75.5,
    note: "En ayunas",
    dateString: "2026-10-31",
    timestampMillis: Date.now()
  }));
  await assertSucceeds(aliceDb.collection("users/alice/weight_logs").doc("w1").get());

  // Other user cannot read alice's weight logs
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  await assertFails(bobDb.collection("users/alice/weight_logs").doc("w1").get());
});

test("Invalid weight log fails validation", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  // Weight <= 0
  await assertFails(aliceDb.collection("users/alice/weight_logs").doc("w_invalid").set({
    id: "w_invalid",
    userId: "alice",
    weightKg: -5,
    dateString: "2026-10-31",
    timestampMillis: Date.now()
  }));
});

test("Authenticated user can manage their workout PRs and weekly nutrition", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users/alice/workout_prs").doc("pr1").set({
    id: "pr1",
    userId: "alice",
    exerciseName: "Press de Banca",
    weightKg: 100.0,
    reps: 5,
    timestampMillis: Date.now()
  }));

  await assertSucceeds(aliceDb.collection("users/alice/weekly_nutrition").doc("2026-W44").set({
    weekKey: "2026-W44",
    userId: "alice",
    totalCalories: 17500,
    avgCaloriesPerDay: 2500,
    daysLogged: 7
  }));
});

test("Authenticated user can manage their routine days and exercises", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await assertSucceeds(aliceDb.collection("users/alice/routine_days").doc("1").set({
    id: "1",
    userId: "alice",
    dayOfWeek: 1,
    dayLabel: "Día 1 · Lunes",
    routineName: "Pierna y Glúteo",
    bodyPart: "Piernas",
    exercises: [
      { id: 1, dayOfWeek: 1, exerciseOrder: 1, name: "Sentadilla", targetSets: 4, targetReps: "10", targetWeightKg: 80.0 }
    ],
    updatedAt: Date.now()
  }));

  await assertSucceeds(aliceDb.collection("users/alice/routine_days").doc("1").get());

  const bobDb = testEnv.authenticatedContext("bob").firestore();
  await assertFails(bobDb.collection("users/alice/routine_days").doc("1").get());
  await assertFails(bobDb.collection("users/alice/routine_days").doc("1").set({
    id: "1",
    userId: "alice",
    dayOfWeek: 1,
    dayLabel: "Día 1 · Lunes",
    routineName: "Hacked",
    bodyPart: "Descanso"
  }));
});
