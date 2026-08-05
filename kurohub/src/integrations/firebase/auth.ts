import {
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  updateProfile,
  signOut as fbSignOut,
  onAuthStateChanged,
  type User as FirebaseUser,
} from "firebase/auth";
import { getFirebaseAuth, getGoogleProvider, isFirebaseConfigured } from "./config";

export type AuthUser = {
  id: string;
  email: string;
  name: string;
  photoURL: string | null;
};

export function getCurrentUser(): Promise<FirebaseUser | null> {
  const auth = getFirebaseAuth();
  if (!auth) return Promise.resolve(null);

  return new Promise((resolve) => {
    const unsub = onAuthStateChanged(auth, (user) => {
      unsub();
      resolve(user);
    });
  });
}

export function mapUser(user: FirebaseUser | null): AuthUser | null {
  if (!user) return null;
  return {
    id: user.uid,
    email: user.email || "",
    name: user.displayName || user.email?.split("@")[0] || "Anime fan",
    photoURL: user.photoURL,
  };
}

export async function signInWithGoogle() {
  const auth = getFirebaseAuth();
  const provider = getGoogleProvider();
  if (!auth || !provider) throw new Error("Firebase not configured");
  const result = await signInWithPopup(auth, provider);
  return mapUser(result.user);
}

export async function signInWithEmail(email: string, password: string) {
  const auth = getFirebaseAuth();
  if (!auth) throw new Error("Firebase not configured");
  const result = await signInWithEmailAndPassword(auth, email, password);
  return mapUser(result.user);
}

export async function signUpWithEmail(email: string, password: string, displayName: string) {
  const auth = getFirebaseAuth();
  if (!auth) throw new Error("Firebase not configured");
  const result = await createUserWithEmailAndPassword(auth, email, password);
  await updateProfile(result.user, { displayName });
  return mapUser(result.user);
}

export async function signOut() {
  const auth = getFirebaseAuth();
  if (!auth) return;
  await fbSignOut(auth);
}

export { onAuthStateChanged, isFirebaseConfigured };