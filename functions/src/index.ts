// Importuj samo ono što koristimo
import {onSchedule} from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import {initializeApp} from "firebase-admin/app";
import {getFirestore, Timestamp} from "firebase-admin/firestore";

// Inicijalizuj Firebase Admin SDK
initializeApp();
const db = getFirestore();

// Definišemo funkciju koja se pokreće svaki dan u 3 ujutru
export const deleteExpiredPartners = onSchedule({
  schedule: "every day 03:00",
  timeZone: "Europe/Belgrade",
  timeoutSeconds: 540,
  memory: "256MiB",
}, async (_event) => { // <-- IZMENA 1: Dodat je underscore ispred 'event'
  logger.info("Starting scheduled deletion of expired activities.");

  const now = Timestamp.now();
  const expiredPartnersQuery = db.collection("training_partners")
    .where("eventTimestamp", "<=", now);

  const snapshot = await expiredPartnersQuery.get();

  if (snapshot.empty) {
    logger.info("No expired activities to delete.");
    return;
  }

  const batch = db.batch();
  snapshot.forEach((doc) => {
    logger.info(`Scheduling deletion for activity: ${doc.id}`);
    batch.delete(doc.ref);
  });

  await batch.commit();
  logger.info(`Successfully deleted ${snapshot.size} expired activities.`);
});
// <-- IZMENA 2: Ostavljen je jedan prazan red na kraju fajla
