import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  addDoc,
  serverTimestamp,
  Timestamp,
  type QuerySnapshot,
  type DocumentData,
} from "firebase/firestore";
import { getFirebaseDb, isFirebaseConfigured } from "./config";

export { isFirebaseConfigured };

export {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  addDoc,
  serverTimestamp,
  Timestamp,
};
export type { QuerySnapshot, DocumentData };

function assertDb() {
  const db = getFirebaseDb();
  if (!db) throw new Error("Firestore not initialized — check Firebase config");
  return db;
}

export async function getDocument<T = DocumentData>(path: string, id: string): Promise<T | null> {
  const db = assertDb();
  const snap = await getDoc(doc(db, path, id));
  return snap.exists() ? (snap.data() as T) : null;
}

export async function setDocument<T extends DocumentData>(path: string, id: string, data: T) {
  const db = assertDb();
  await setDoc(doc(db, path, id), data, { merge: true });
}

export async function queryCollection<T = DocumentData>(
  path: string,
  ...constraints: unknown[]
): Promise<T[]> {
  const db = assertDb();
  const q = query(collection(db, path), ...constraints);
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }) as T);
}