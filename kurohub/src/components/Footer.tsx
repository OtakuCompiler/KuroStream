import { Link } from "@tanstack/react-router";
import { Github, Twitter, MessageCircle, Heart, ArrowUpRight, Mail } from "lucide-react";
import { m } from "framer-motion";
import { useState } from "react";
import logo from "@/assets/kuro-logo.png";

const footerLinks = {
  Product: [
    { label: "Features", to: "/features" },
    { label: "Download", to: "/download" },
    { label: "Setup Guide", to: "/setup" },
    { label: "Marketplace", to: "/marketplace" },
  ],
  Developers: [
    { label: "Documentation", to: "/docs" },
    { label: "Contribute", to: "/contribute" },
    { label: "GitHub", href: "https://github.com/OtakuCompiler/KuroStream" },
    { label: "Extensions", to: "/docs" },
  ],
  Legal: [
    { label: "Privacy", to: "/legal" },
    { label: "Terms", to: "/legal" },
    {
      label: "GPL-3.0 License",
      href: "https://github.com/OtakuCompiler/KuroStream/blob/main/LICENSE",
    },
  ],
};

export function Footer() {
  const [email, setEmail] = useState("");
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) {
      setSubscribed(true);
      setEmail("");
      setTimeout(() => setSubscribed(false), 3000);
    }
  };

  return (
    <footer className="relative border-t border-border/10 pt-16 pb-8">
      <div className="mx-auto max-w-6xl px-5">
        <m.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="grid md:grid-cols-5 gap-10 mb-14"
        >
          {/* Brand */}
          <div className="md:col-span-2">
            <Link to="/" className="flex items-center gap-2.5 mb-4">
              <img src={logo} alt="" className="w-7 h-7 rounded-md" />
              <span className="font-display font-semibold text-sm tracking-tight">
                KURO <span className="text-gradient">STREAM</span>
              </span>
            </Link>
            <p className="text-sm text-muted-foreground leading-relaxed max-w-xs mb-6">
              The open-source anime streaming app for Android TV. Zero telemetry. Community-powered.
            </p>
            <div className="flex items-center gap-3">
              {[
                {
                  icon: Github,
                  href: "https://github.com/OtakuCompiler/KuroStream",
                  label: "GitHub",
                },
                { icon: Twitter, href: "#", label: "Twitter" },
                { icon: MessageCircle, href: "#", label: "Discord" },
              ].map(({ icon: Icon, href, label }) => (
                <m.a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noreferrer"
                  whileHover={{ scale: 1.15, y: -2 }}
                  whileTap={{ scale: 0.95 }}
                  className="w-9 h-9 rounded-lg bg-surface-2 border border-stroke-soft flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-primary/30 transition-colors"
                  aria-label={label}
                >
                  <Icon className="w-4 h-4" />
                </m.a>
              ))}
            </div>
          </div>

          {/* Links */}
          {Object.entries(footerLinks).map(([title, links]) => (
            <div key={title}>
              <h4 className="font-display font-semibold text-xs uppercase tracking-wider text-muted-foreground mb-4">
                {title}
              </h4>
              <ul className="space-y-2.5">
                {links.map((link) => (
                  <li key={link.label}>
                    {"to" in link ? (
                      <Link
                        to={link.to}
                        className="text-sm text-muted-foreground hover:text-foreground transition-colors inline-flex items-center gap-1 group"
                      >
                        {link.label}
                        <ArrowUpRight className="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                      </Link>
                    ) : (
                      <a
                        href={link.href}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm text-muted-foreground hover:text-foreground transition-colors inline-flex items-center gap-1 group"
                      >
                        {link.label}
                        <ArrowUpRight className="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity" />
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </m.div>

        {/* Newsletter */}
        <m.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="glass-card p-6 mb-10 flex flex-col md:flex-row items-center justify-between gap-4"
        >
          <div>
            <h4 className="font-display font-semibold text-sm mb-1">Stay in the loop</h4>
            <p className="text-xs text-muted-foreground">
              Get updates on new skins, features, and releases.
            </p>
          </div>
          <form onSubmit={handleSubscribe} className="flex gap-2 w-full md:w-auto">
            <div className="relative flex-1 md:w-64">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/50" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full pl-10 pr-4 py-2.5 bg-input/60 border border-border/50 rounded-lg text-sm focus:ring-2 focus:ring-primary/40 focus:border-primary/40 transition-all outline-none"
                required
              />
            </div>
            <m.button
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.97 }}
              type="submit"
              className="px-5 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors whitespace-nowrap"
            >
              {subscribed ? "Subscribed!" : "Subscribe"}
            </m.button>
          </form>
        </m.div>

        {/* Bottom bar */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-4 pt-6 border-t border-border/10">
          <p className="text-xs text-muted-foreground/60 flex items-center gap-1">
            Built with <Heart className="w-3 h-3 text-destructive fill-destructive" /> by the
            community. GPL-3.0.
          </p>
          <p className="text-xs text-muted-foreground/40">
            © {new Date().getFullYear()} KuroStream. Not affiliated with any content providers.
          </p>
        </div>
      </div>
    </footer>
  );
}
