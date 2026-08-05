// Firebase client-side initialization — lazy, SSR-safe
import { initializeApp, getApps, getApp, type FirebaseApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, type Auth } from "firebase/auth";
import { getFirestore, type Firestore } from "firebase/firestore";
import { getStorage, type FirebaseStorage } from "firebase/storage";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "",
};

function hasValidConfig(): boolean {
  return (
    typeof window !== "undefined" &&
    firebaseConfig.apiKey &&
    firebaseConfig.authDomain &&
    firebaseConfig.projectId
  );
}

let appInstance: FirebaseApp | null = null;
let authInstance: Auth | null = null;
let dbInstance: Firestore | null = null;
let storageInstance: FirebaseStorage | null = null;
let googleProviderInstance: GoogleAuthProvider | null = null;

function initializeFirebase(): void {
  if (appInstance) return;
  if (!hasValidConfig()) return;

  appInstance = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
  authInstance = getAuth(appInstance);
  dbInstance = getFirestore(appInstance);
  storageInstance = getStorage(appInstance);
  googleProviderInstance = new GoogleAuthProvider();
}

export function getFirebaseApp(): FirebaseApp | null {
  initializeFirebase();
  return appInstance;
}

export function getFirebaseAuth(): Auth | null {
  initializeFirebase();
  return authInstance;
}

export function getFirebaseDb(): Firestore | null {
  initializeFirebase();
  return dbInstance;
}

export function getFirebaseStorage(): FirebaseStorage | null {
  initializeFirebase();
  return storageInstance;
}

export function getGoogleProvider(): GoogleAuthProvider | null {
  initializeFirebase();
  return googleProviderInstance;
}

export function isFirebaseConfigured(): boolean {
  return hasValidConfig();
}

export { googleProviderInstance as googleProvider };