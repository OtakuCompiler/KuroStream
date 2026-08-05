import { Link, useLocation, useNavigate } from "@tanstack/react-router";
import { Menu, X, User, LogOut, Library as LibraryIcon, Github, Sparkles } from "lucide-react";
import { useEffect, useState, useRef, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { useUser } from "@/lib/marketplace";
import { m, AnimatePresence, useScroll, useTransform, useMotionValueEvent } from "framer-motion";
import logo from "@/assets/kuro-logo.png";

const links = [
  { to: "/", label: "Home" },
  { to: "/marketplace", label: "Marketplace" },
  { to: "/platforms", label: "Platforms" },
  { to: "/docs", label: "Docs" },
  { to: "/download", label: "Download" },
] as const;

export function Navbar() {
  const { pathname } = useLocation();
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [hidden, setHidden] = useState(false);
  const { user, signOut } = useUser();
  const navigate = useNavigate();
  const lastScrollY = useRef(0);

  const { scrollY } = useScroll();
  const maxScroll =
    typeof document !== "undefined"
      ? document.body?.scrollHeight - (window?.innerHeight ?? 0) || 1000
      : 1000;
  const scrollProgress = useTransform(scrollY, [0, maxScroll], [0, 1]);

  useMotionValueEvent(scrollY, "change", (latest) => {
    const direction = latest > lastScrollY.current ? "down" : "up";
    if (direction === "down" && latest > 100) setHidden(true);
    else setHidden(false);
    lastScrollY.current = latest;
    setScrolled(latest > 12);
  });

  useEffect(() => {
    if (open) document.body.style.overflow = "hidden";
    else document.body.style.overflow = "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  const handleSignOut = useCallback(async () => {
    await signOut();
    navigate({ to: "/" });
  }, [signOut, navigate]);

  return (
    <>
      {/* Scroll Progress */}
      <m.div
        className="scroll-progress"
        style={{ scaleX: scrollProgress, transformOrigin: "left" }}
      />

      <m.header
        initial={{ y: -100 }}
        animate={{ y: hidden ? -100 : 0 }}
        transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
        className="fixed top-0 left-0 right-0 z-50"
      >
        <nav
          className={`mx-auto flex items-center justify-between transition-all duration-500 ${
            scrolled
              ? "max-w-5xl mx-3 md:mx-auto mt-3 af-panel-strong px-5 h-14"
              : "max-w-6xl px-5 h-16 border-b border-transparent"
          }`}
        >
          <Link
            to="/"
            className="focusable flex items-center gap-2.5 group"
            aria-label="Kuro Stream home"
          >
            <m.img
              src={logo}
              alt=""
              aria-hidden
              className="w-8 h-8 rounded-lg ring-1 ring-primary/40"
              whileHover={{ rotate: [0, -10, 10, 0], scale: 1.1 }}
              transition={{ duration: 0.5 }}
              style={{
                boxShadow: "0 0 20px -4px oklch(0.55 0.24 295/0.6)",
              }}
            />
            <span className="font-display font-semibold text-base tracking-tight">
              KURO <span className="text-gradient">STREAM</span>
            </span>
          </Link>

          <ul className="hidden md:flex items-center gap-1">
            {links.map((l) => {
              const active = pathname === l.to;
              return (
                <li key={l.to}>
                  <Link
                    to={l.to}
                    className={`focusable relative px-3 py-1.5 rounded-full text-[13px] transition-all duration-300 ${
                      active
                        ? "text-foreground bg-white/5"
                        : "text-muted-foreground hover:text-foreground hover:bg-white/[0.03]"
                    }`}
                  >
                    {l.label}
                    {active && (
                      <m.span
                        layoutId="nav-indicator"
                        className="absolute left-1/2 -translate-x-1/2 -bottom-1 w-1 h-1 rounded-full bg-primary"
                        style={{ boxShadow: "0 0 8px var(--primary)" }}
                        transition={{ type: "spring", stiffness: 380, damping: 30 }}
                      />
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>

          <div className="hidden md:flex items-center gap-2">
            <a
              href="https://github.com/OtakuCompiler/KuroStream"
              target="_blank"
              rel="noreferrer"
              className="focusable p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all duration-300"
              aria-label="GitHub"
            >
              <Github className="w-4 h-4" />
            </a>
            {user ? (
              <div className="flex items-center gap-2">
                <Link
                  to="/account"
                  className="focusable flex items-center gap-2 px-3 py-1.5 rounded-full text-sm text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all duration-300"
                >
                  <User className="w-4 h-4" />
                  <span className="max-w-[100px] truncate">{user.name}</span>
                </Link>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={handleSignOut}
                  className="text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-all"
                >
                  <LogOut className="w-4 h-4" />
                </Button>
              </div>
            ) : (
              <Button
                asChild
                size="sm"
                className="h-8 px-4 text-sm bg-primary/20 text-primary hover:bg-primary/30 border border-primary/30 transition-all hover:scale-105"
              >
                <Link to="/auth">
                  <Sparkles className="w-3.5 h-3.5 mr-1" /> Sign in
                </Link>
              </Button>
            )}
          </div>

          <button
            onClick={() => setOpen(!open)}
            className="md:hidden focusable p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all"
            aria-label="Toggle menu"
          >
            <AnimatePresence mode="wait">
              {open ? (
                <m.div
                  key="close"
                  initial={{ rotate: -90, opacity: 0 }}
                  animate={{ rotate: 0, opacity: 1 }}
                  exit={{ rotate: 90, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <X className="w-5 h-5" />
                </m.div>
              ) : (
                <m.div
                  key="menu"
                  initial={{ rotate: 90, opacity: 0 }}
                  animate={{ rotate: 0, opacity: 1 }}
                  exit={{ rotate: -90, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <Menu className="w-5 h-5" />
                </m.div>
              )}
            </AnimatePresence>
          </button>
        </nav>

        {/* Mobile menu overlay */}
        <AnimatePresence>
          {open && (
            <>
              <m.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 bg-background/80 backdrop-blur-sm z-40 md:hidden"
                onClick={() => setOpen(false)}
              />
              <m.div
                initial={{ opacity: 0, y: -20, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -20, scale: 0.95 }}
                transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                className="md:hidden absolute top-full left-4 right-4 mt-2 af-panel-strong p-4 flex flex-col gap-1 z-50"
              >
                {links.map((l, i) => (
                  <m.div
                    key={l.to}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05, duration: 0.3 }}
                  >
                    <Link
                      to={l.to}
                      onClick={() => setOpen(false)}
                      className={`px-3 py-2.5 rounded-lg text-sm block transition-all ${pathname === l.to ? "text-foreground bg-white/5 font-medium" : "text-muted-foreground hover:text-foreground hover:bg-white/[0.03]"}`}
                    >
                      {l.label}
                    </Link>
                  </m.div>
                ))}
                <hr className="border-border/20 my-2" />
                {user ? (
                  <>
                    <m.div
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.25 }}
                    >
                      <Link
                        to="/account"
                        onClick={() => setOpen(false)}
                        className="px-3 py-2.5 rounded-lg text-sm flex items-center gap-2 text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all"
                      >
                        <User className="w-4 h-4" /> Account
                      </Link>
                    </m.div>
                    <m.div
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.3 }}
                    >
                      <Link
                        to="/library"
                        onClick={() => setOpen(false)}
                        className="px-3 py-2.5 rounded-lg text-sm flex items-center gap-2 text-muted-foreground hover:text-foreground hover:bg-white/5 transition-all"
                      >
                        <LibraryIcon className="w-4 h-4" /> Library
                      </Link>
                    </m.div>
                    <m.div
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.35 }}
                    >
                      <button
                        onClick={() => {
                          handleSignOut();
                          setOpen(false);
                        }}
                        className="px-3 py-2.5 rounded-lg text-sm text-left flex items-center gap-2 text-destructive hover:bg-destructive/10 transition-all w-full"
                      >
                        <LogOut className="w-4 h-4" /> Sign out
                      </button>
                    </m.div>
                  </>
                ) : (
                  <m.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.25 }}
                  >
                    <Link
                      to="/auth"
                      onClick={() => setOpen(false)}
                      className="px-3 py-2.5 rounded-lg text-sm text-primary hover:bg-white/5 transition-all block"
                    >
                      Sign in
                    </Link>
                  </m.div>
                )}
              </m.div>
            </>
          )}
        </AnimatePresence>
      </m.header>
    </>
  );
}
