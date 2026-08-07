declare module "framer-motion" {
  export const m: any;
  export const motion: any;
  export const AnimatePresence: any;
  export function useScroll(options?: any): any;
  export function useTransform(value: any, inputRange: number[], outputRange: any[]): any;
  export function useMotionValueEvent(value: any, event: string, callback: (v: any) => void): void;
  export function useInView(ref: any, options?: any): any;
  export const LazyMotion: any;
  export const domAnimation: any;
}

declare module "lucide-react" {
  import { FC, SVGProps } from "react";
  export type LucideIcon = FC<SVGProps<SVGSVGElement>>;
  export const AlertTriangle: LucideIcon;
  export const ArrowLeft: LucideIcon;
  export const ArrowRight: LucideIcon;
  export const ArrowUpRight: LucideIcon;
  export const BarChart3: LucideIcon;
  export const BookOpen: LucideIcon;
  export const Bug: LucideIcon;
  export const Calendar: LucideIcon;
  export const Cast: LucideIcon;
  export const Check: LucideIcon;
  export const CheckCircle2: LucideIcon;
  export const ChevronDown: LucideIcon;
  export const ChevronDownIcon: LucideIcon;
  export const ChevronLeft: LucideIcon;
  export const ChevronLeftIcon: LucideIcon;
  export const ChevronRight: LucideIcon;
  export const ChevronRightIcon: LucideIcon;
  export const ChevronUp: LucideIcon;
  export const Chrome: LucideIcon;
  export const Circle: LucideIcon;
  export const Clapperboard: LucideIcon;
  export const Clock: LucideIcon;
  export const Code2: LucideIcon;
  export const Cpu: LucideIcon;
  export const Disc: LucideIcon;
  export const DollarSign: LucideIcon;
  export const Download: LucideIcon;
  export const ExternalLink: LucideIcon;
  export const Eye: LucideIcon;
  export const EyeOff: LucideIcon;
  export const FileCode: LucideIcon;
  export const Film: LucideIcon;
  export const Filter: LucideIcon;
  export const Gauge: LucideIcon;
  export const GitBranch: LucideIcon;
  export const GitPullRequest: LucideIcon;
  export const Github: LucideIcon;
  export const Globe: LucideIcon;
  export const Grid3X3: LucideIcon;
  export const GripVertical: LucideIcon;
  export const HardDrive: LucideIcon;
  export const Heart: LucideIcon;
  export const HelpCircle: LucideIcon;
  export const Infinity: LucideIcon;
  export const Layers: LucideIcon;
  export const Library: LucideIcon;
  export const List: LucideIcon;
  export const Lock: LucideIcon;
  export const LogOut: LucideIcon;
  export const Mail: LucideIcon;
  export const Menu: LucideIcon;
  export const MessageCircle: LucideIcon;
  export const MessageSquare: LucideIcon;
  export const Minus: LucideIcon;
  export const Monitor: LucideIcon;
  export const MonitorPlay: LucideIcon;
  export const MoreHorizontal: LucideIcon;
  export const Network: LucideIcon;
  export const Package: LucideIcon;
  export const Palette: LucideIcon;
  export const PanelLeft: LucideIcon;
  export const Play: LucideIcon;
  export const Plug: LucideIcon;
  export const Puzzle: LucideIcon;
  export const Quote: LucideIcon;
  export const Radio: LucideIcon;
  export const Receipt: LucideIcon;
  export const RefreshCcw: LucideIcon;
  export const Repeat: LucideIcon;
  export const Search: LucideIcon;
  export const Settings: LucideIcon;
  export const Shield: LucideIcon;
  export const ShieldCheck: LucideIcon;
  export const ShoppingCart: LucideIcon;
  export const SkipForward: LucideIcon;
  export const Smartphone: LucideIcon;
  export const Sparkles: LucideIcon;
  export const Star: LucideIcon;
  export const Subtitles: LucideIcon;
  export const Terminal: LucideIcon;
  export const Tv: LucideIcon;
  export const Tv2: LucideIcon;
  export const Twitter: LucideIcon;
  export const Upload: LucideIcon;
  export const User: LucideIcon;
  export const Wifi: LucideIcon;
  export const X: LucideIcon;
  export const Zap: LucideIcon;
}

declare module "stripe" {
  namespace Stripe {
    export interface Event {
      type: string;
      data: { object: any };
    }
    export namespace Checkout {
      export interface Session {
        id: string;
        url?: string;
        client_reference_id?: string;
        metadata?: Record<string, string>;
      }
    }
  }
  class Stripe {
    constructor(secretKey: string, options?: { apiVersion?: string });
    checkout: {
      sessions: {
        create(params: any): Promise<Stripe.Checkout.Session>;
      };
    };
    webhooks: {
      constructEvent(body: string, sig: string, secret: string): Stripe.Event;
    };
  }
  export default Stripe;
}

declare module "firebase/app" {
  export function initializeApp(config: any): any;
  export function getApps(): any[];
  export function getApp(): any;
}
declare module "firebase/auth" {
  export interface User {
    uid: string;
    email: string | null;
    displayName: string | null;
    photoURL: string | null;
  }
  export function signInWithPopup(auth: any, provider: any): Promise<{ user: User }>;
  export function signInWithEmailAndPassword(
    auth: any,
    email: string,
    password: string,
  ): Promise<{ user: User }>;
  export function createUserWithEmailAndPassword(
    auth: any,
    email: string,
    password: string,
  ): Promise<{ user: User }>;
  export function updateProfile(
    user: User,
    profile: { displayName?: string; photoURL?: string },
  ): Promise<void>;
  export function signOut(auth: any): Promise<void>;
  export function onAuthStateChanged(auth: any, callback: (user: User | null) => void): () => void;
  export function getAuth(app: any): any;
  export class GoogleAuthProvider {
    constructor();
  }
}
declare module "firebase/firestore" {
  export function getFirestore(app: any): any;
  export function collection(db: any, path: string): any;
  export function doc(db: any, path: string, ...pathSegments: any[]): any;
  export function getDoc(docRef: any): Promise<any>;
  export function getDocs(queryRef: any): Promise<QuerySnapshot<any>>;
  export function setDoc(docRef: any, data: any, options?: any): Promise<void>;
  export function updateDoc(docRef: any, data: any): Promise<void>;
  export function deleteDoc(docRef: any): Promise<void>;
  export function query(ref: any, ...constraints: any[]): any;
  export function where(field: string, op: string, value: any): any;
  export function orderBy(field: string, direction?: string): any;
  export function addDoc(col: any, data: any): Promise<any>;
  export function serverTimestamp(): any;
  export const Timestamp: any;
  export type DocumentData = Record<string, any>;
  export interface QueryDocumentSnapshot<T = DocumentData> {
    id: string;
    exists(): boolean;
    data(): T;
  }
  export interface QuerySnapshot<T = DocumentData> {
    docs: QueryDocumentSnapshot<T>[];
  }
}
declare module "firebase/storage" {
  export function getStorage(app: any): any;
}

declare module "@cloudflare/workers-types" {
  export interface D1Database {
    prepare(query: string): {
      bind(...values: any[]): {
        first<T>(): Promise<T | null>;
        all<T>(): Promise<{ results: T[] }>;
        run(): Promise<any>;
      };
    };
  }
  export interface KVNamespace {
    get(key: string, options?: { type: "json" }): Promise<any>;
    put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void>;
  }
}

declare module "cloudflare:workers" {
  export interface DurableObjectState {
    id: { id: string; toString: () => string };
    waitUntil(promise: Promise<any>): void;
  }
  export interface ExecutionContext {
    waitUntil(promise: Promise<any>): void;
    passThroughOnException(): void;
  }
  export interface WebSocket {
    readonly readyState: number;
    accept(): void;
    send(data: string | ArrayBuffer | Blob): void;
    close(code?: number, reason?: string): void;
    addEventListener(type: string, listener: (event: any) => void): void;
  }
  export interface WebSocketPair {
    0: WebSocket;
    1: WebSocket;
  }
  export { WebSocket, WebSocketPair, DurableObjectState, ExecutionContext };
}

declare module "react-day-picker" {
  export const DayPicker: any;
  export const DayButton: any;
  export function getDefaultClassNames(): any;
}
