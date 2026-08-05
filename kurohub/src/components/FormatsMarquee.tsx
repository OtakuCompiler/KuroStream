import {
  Film,
  MonitorPlay,
  Radio,
  Tv,
  Clapperboard,
  Disc,
  HardDrive,
  Globe,
  Wifi,
  Cast,
} from "lucide-react";

const formats = [
  { icon: Film, label: "MKV" },
  { icon: MonitorPlay, label: "MP4" },
  { icon: Radio, label: "HLS" },
  { icon: Tv, label: "DASH" },
  { icon: Clapperboard, label: "RTSP" },
  { icon: Disc, label: "WebM" },
  { icon: HardDrive, label: "AVI" },
  { icon: Globe, label: "Cloud Drive" },
  { icon: Wifi, label: "M3U8" },
  { icon: Cast, label: "Chromecast" },
];

export function FormatsMarquee() {
  const items = [...formats, ...formats];

  return (
    <section className="relative py-8 overflow-hidden border-y border-border/10">
      <div className="absolute left-0 top-0 bottom-0 w-24 bg-gradient-to-r from-background to-transparent z-10 pointer-events-none" />
      <div className="absolute right-0 top-0 bottom-0 w-24 bg-gradient-to-l from-background to-transparent z-10 pointer-events-none" />
      <div className="marquee-track">
        {items.map(({ icon: Icon, label }, i) => (
          <div
            key={`${label}-${i}`}
            className="flex items-center gap-3 px-8 text-muted-foreground/60 hover:text-muted-foreground transition-colors duration-300"
          >
            <Icon className="w-5 h-5" />
            <span className="text-sm font-medium whitespace-nowrap">{label}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
