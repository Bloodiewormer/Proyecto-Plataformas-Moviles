import { useState, type ReactNode, type CSSProperties } from "react"

// Tracks which role's home screen to return to from shared screens (H2, K1, K2)
let roleHome = "B2"

// ── tokens ──────────────────────────────────────────────────────────────────
const NIGHT = {
  background: "#161E27", surface: "#2E3B4B", surfaceHigh: "#3B4A5C", border: "#4A5A6E",
  accent: "#FFD372", accentText: "#FFD372", onAccent: "#1A1206",
  accentSoft: "rgba(255,211,114,.16)", accentFaint: "rgba(255,211,114,.08)", accentLine: "rgba(255,211,114,.42)",
  textPrimary: "#D7D1B9", textSecondary: "#959595",
  alert: "#E0693A", alertSoft: "rgba(224,105,58,.18)", alertFaint: "rgba(224,105,58,.09)", alertLine: "rgba(224,105,58,.45)",
  verified: "#5FA88C", verifiedSoft: "rgba(95,168,140,.18)", verifiedLine: "rgba(95,168,140,.45)",
  repaired: "#8FB7DC", repairedSoft: "rgba(143,183,220,.18)", repairedLine: "rgba(143,183,220,.45)",
  escalated: "#F59E0B", escalatedSoft: "rgba(245,158,11,.18)", escalatedFaint: "rgba(245,158,11,.09)", escalatedLine: "rgba(245,158,11,.45)",
  uncertain: "#959595", uncertainSoft: "rgba(149,149,149,.20)", uncertainFaint: "rgba(149,149,149,.10)", uncertainLine: "rgba(149,149,149,.45)",
  neutralSoft: "rgba(149,149,149,.18)", btnSecBorder: "#FFD372", btnSecText: "#FFD372",
  scrim: "rgba(8,12,17,.72)",
}
const DAY = {
  background: "#EDEAE0", surface: "#F7F4EC", surfaceHigh: "#D7D1B9", border: "#C4BCA3",
  accent: "#FFD372", accentText: "#8A6210", onAccent: "#2E3B4B",
  accentSoft: "rgba(196,143,20,.20)", accentFaint: "rgba(196,143,20,.10)", accentLine: "rgba(160,116,15,.5)",
  textPrimary: "#2E3B4B", textSecondary: "#63666A",
  alert: "#B94117", alertSoft: "rgba(185,65,23,.14)", alertFaint: "rgba(185,65,23,.07)", alertLine: "rgba(185,65,23,.42)",
  verified: "#2F7D62", verifiedSoft: "rgba(47,125,98,.14)", verifiedLine: "rgba(47,125,98,.42)",
  repaired: "#3E6E9E", repairedSoft: "rgba(62,110,158,.14)", repairedLine: "rgba(62,110,158,.42)",
  escalated: "#D97706", escalatedSoft: "rgba(217,119,6,.14)", escalatedFaint: "rgba(217,119,6,.07)", escalatedLine: "rgba(217,119,6,.42)",
  uncertain: "#63666A", uncertainSoft: "rgba(99,102,106,.14)", uncertainFaint: "rgba(99,102,106,.07)", uncertainLine: "rgba(99,102,106,.38)",
  neutralSoft: "rgba(99,102,106,.14)", btnSecBorder: "#A07413", btnSecText: "#8A6210",
  scrim: "rgba(46,59,75,.5)",
}
type Tok = typeof NIGHT
type Nav = (screen: string) => void

// ── svg symbols ──────────────────────────────────────────────────────────────
function Defs() {
  return (
    <svg style={{ display: "none" }} xmlns="http://www.w3.org/2000/svg" aria-hidden>
      <symbol id="menu" viewBox="0 0 24 24"><path d="M3 6h18M3 12h18M3 18h18" /></symbol>
      <symbol id="back" viewBox="0 0 24 24"><path d="M19 12H5M12 19l-7-7 7-7" /></symbol>
      <symbol id="close" viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18" /></symbol>
      <symbol id="search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7" /><path d="M20.5 20.5L16.2 16.2" /></symbol>
      <symbol id="add" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></symbol>
      <symbol id="camera" viewBox="0 0 24 24"><path d="M3 8.5A2.5 2.5 0 015.5 6H8l1.4-2.2h5.2L16 6h2.5A2.5 2.5 0 0121 8.5v9A2.5 2.5 0 0118.5 20h-13A2.5 2.5 0 013 17.5z" /><circle cx="12" cy="13" r="3.8" /></symbol>
      <symbol id="home" viewBox="0 0 24 24"><path d="M3.5 10.6L12 3.5l8.5 7.1V20a1 1 0 01-1 1h-15a1 1 0 01-1-1z" /><path d="M9.5 21v-6.5h5V21" /></symbol>
      <symbol id="school" viewBox="0 0 24 24"><path d="M12 4L2.5 8.6 12 13.2l9.5-4.6z" /><path d="M6.5 10.8v4.4c0 1.8 2.5 3.1 5.5 3.1s5.5-1.3 5.5-3.1v-4.4" /></symbol>
      <symbol id="notes" viewBox="0 0 24 24"><path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z" /><path d="M14 3v5h5" /><path d="M8.8 13h6.4M8.8 16.6h4.2" /></symbol>
      <symbol id="coverage" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5" /><path d="M12 3.5v8.5h8.5" /></symbol>
      <symbol id="study" viewBox="0 0 24 24"><path d="M12 3.2l8.8 4.6L12 12.4 3.2 7.8z" /><path d="M3.2 13.4L12 18l8.8-4.6" /></symbol>
      <symbol id="sync" viewBox="0 0 24 24"><path d="M20.2 11a8.2 8.2 0 00-14-5.4L3.8 8" /><path d="M3.8 13a8.2 8.2 0 0014 5.4L20.2 16" /><path d="M3.8 3.6V8h4.4M20.2 20.4V16h-4.4" /></symbol>
      <symbol id="person" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.8" /><path d="M4.5 20.6v-.8a5.4 5.4 0 015.4-5.4h4.2a5.4 5.4 0 015.4 5.4v.8" /></symbol>
      <symbol id="settings" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3.1" /><path d="M12 2.6v3M12 18.4v3M2.6 12h3M18.4 12h3M5.4 5.4l2.1 2.1M16.5 16.5l2.1 2.1M18.6 5.4l-2.1 2.1M7.5 16.5l-2.1 2.1" /></symbol>
      <symbol id="logout" viewBox="0 0 24 24"><path d="M16 17l5-5-5-5" /><path d="M21 12H9" /><path d="M9 3.4H5.4a2 2 0 00-2 2v13.2a2 2 0 002 2H9" /></symbol>
      <symbol id="syllabus" viewBox="0 0 24 24"><path d="M8.5 6.5h12M8.5 12h12M8.5 17.5h12" /><path d="M3.8 6.5h.02M3.8 12h.02M3.8 17.5h.02" strokeWidth="2.4" /></symbol>
      <symbol id="glossary" viewBox="0 0 24 24"><path d="M18.5 4.5H6l6.6 7.5L6 19.5h12.5" /></symbol>
      <symbol id="gaps" viewBox="0 0 24 24"><path d="M3.4 17.2l5.4-5.4 3.6 3.6 6.4-7.4" /><path d="M15.4 7.4h4.4v4.4" /></symbol>
      <symbol id="copy" viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5.6 15H4.8a1.8 1.8 0 01-1.8-1.8V4.8A1.8 1.8 0 014.8 3h8.4A1.8 1.8 0 0115 4.8v.8" /></symbol>
      <symbol id="down" viewBox="0 0 24 24"><path d="M6.5 9.5l5.5 5.5 5.5-5.5" /></symbol>
      <symbol id="right" viewBox="0 0 24 24"><path d="M9.5 5.5L16 12l-6.5 6.5" /></symbol>
      <symbol id="warn" viewBox="0 0 24 24"><path d="M12 3.6L21.2 19.8H2.8z" /><path d="M12 10v4M12 16.8h.02" strokeWidth="2" /></symbol>
      <symbol id="clock" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.6" /><path d="M12 7v5.2l3.2 2" /></symbol>
      <symbol id="check" viewBox="0 0 24 24"><path d="M4.5 12.5l5 5 10-11" /></symbol>
      <symbol id="users" viewBox="0 0 24 24"><circle cx="9.5" cy="8.4" r="3.4" /><path d="M3 20.4v-.9a4.9 4.9 0 014.9-4.9h3.2a4.9 4.9 0 014.9 4.9v.9" /><path d="M16.4 5.6a3.2 3.2 0 010 6.2M17.6 14.8h.7a4 4 0 014 4v1.6" /></symbol>
      <symbol id="image" viewBox="0 0 24 24"><rect x="3" y="4.5" width="18" height="15" rx="2" /><circle cx="8.4" cy="9.6" r="1.8" /><path d="M3.6 17.4l5-5 4.4 4.4 3-2.8 4.4 4.2" /></symbol>
      <symbol id="flash" viewBox="0 0 24 24"><path d="M13.5 2.5L5 13.5h5.5L10 21.5 19 10.5h-5.5z" /></symbol>
      <symbol id="edit" viewBox="0 0 24 24"><path d="M4 20h4.2L19.4 8.8a2.1 2.1 0 00-3-3L5.2 17z" /><path d="M14.6 6.2l3.2 3.2" /></symbol>
      <symbol id="refresh" viewBox="0 0 24 24"><path d="M20.5 12a8.5 8.5 0 11-2.6-6.1" /><path d="M20.5 4v5h-5" /></symbol>
      <symbol id="shield" viewBox="0 0 24 24"><path d="M12 3l7.5 3v5.5c0 4.4-3 8.4-7.5 9.7-4.5-1.3-7.5-5.3-7.5-9.7V6z" /><path d="M9 12.2l2.2 2.2 4-4.2" /></symbol>
      <symbol id="calendar" viewBox="0 0 24 24"><rect x="3.5" y="5" width="17" height="15.5" rx="2" /><path d="M3.5 9.6h17M8.2 3v4M15.8 3v4" /></symbol>
      <symbol id="cost" viewBox="0 0 24 24"><path d="M3.5 20.5h17" /><path d="M6.6 20.5v-5.4M11.4 20.5V6.2M16.2 20.5v-8.8" /></symbol>
      <symbol id="target" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.6" /><circle cx="12" cy="12" r="4.4" /><circle cx="12" cy="12" r=".8" strokeWidth="2" /></symbol>
      <symbol id="sys-wifi" viewBox="0 0 24 18"><path d="M12 17.4L23.2 4.2A17.4 17.4 0 00.8 4.2z" /></symbol>
      <symbol id="sys-signal" viewBox="0 0 24 18"><path d="M22 1.2v15.4a.9.9 0 01-.9.9H2.6c-.8 0-1.2-1-.6-1.5L20.5.6c.6-.5 1.5-.1 1.5.6z" /></symbol>
      <symbol id="sys-batt" viewBox="0 0 26 14"><rect x=".8" y=".8" width="21.4" height="12.4" rx="3.4" fill="none" stroke="currentColor" strokeWidth="1.6" /><rect x="3.2" y="3.2" width="14" height="7.6" rx="1.8" /><path d="M24.2 5.2v3.6" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" /></symbol>
    </svg>
  )
}

// ── primitives ───────────────────────────────────────────────────────────────
function Ic({ id, s = 24, style }: { id: string; s?: number; style?: CSSProperties }) {
  return (
    <svg width={s} height={s} fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, ...style }}>
      <use href={`#${id}`} />
    </svg>
  )
}
function Icf({ id, w, h }: { id: string; w: number; h: number }) {
  return <svg width={w} height={h} style={{ fill: "currentColor", flexShrink: 0 }}><use href={`#${id}`} /></svg>
}

// ── Android shell components ─────────────────────────────────────────────────
function SysBar({ t }: { t: Tok }) {
  return (
    <div style={{ height: 24, display: "flex", alignItems: "center", padding: "0 12px", flexShrink: 0, color: t.textPrimary }}>
      <span style={{ fontSize: 11, fontWeight: 600 }}>9:41</span>
      <div style={{ flex: 1 }} />
      <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
        <Icf id="sys-wifi" w={12} h={9} />
        <Icf id="sys-signal" w={12} h={9} />
        <Icf id="sys-batt" w={18} h={10} />
      </div>
    </div>
  )
}
function GestureBar({ t }: { t: Tok }) {
  return (
    <div style={{ height: 22, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
      <div style={{ width: 90, height: 3, borderRadius: 999, background: t.textSecondary, opacity: .65 }} />
    </div>
  )
}
function AppBar({ t, left, title, right, sub, children }: { t: Tok; left?: ReactNode; title?: string; sub?: string; right?: ReactNode; children?: ReactNode }) {
  return (
    <div style={{ height: 48, background: t.surface, borderBottom: `1px solid ${t.border}`, display: "flex", alignItems: "center", padding: "0 6px 0 10px", gap: 8, flexShrink: 0 }}>
      {left}
      {children ?? (sub
        ? <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.2, paddingLeft: 3 }}>
            <span style={{ fontSize: 10, color: t.textSecondary, letterSpacing: ".3px" }}>{sub}</span>
            <span style={{ fontSize: 16, fontWeight: 500, display: "flex", alignItems: "center", gap: 3 }}>
              {title} <Ic id="down" s={16} style={{ strokeWidth: 2.5, color: t.textPrimary }} />
            </span>
          </div>
        : <span style={{ fontSize: title && title.length > 14 ? 16 : 18, fontWeight: 500, paddingLeft: sub ? 0 : 3 }}>{title}</span>)}
      {!children && <div style={{ flex: 1 }} />}
      {right}
    </div>
  )
}
function Tap({ t, children, onClick }: { t: Tok; children: ReactNode; onClick?: () => void }) {
  return (
    <div onClick={onClick} style={{ width: 36, height: 36, borderRadius: 999, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary, cursor: "pointer", flexShrink: 0 }}>
      {children}
    </div>
  )
}
function Banner({ t, warn, children }: { t: Tok; warn?: boolean; children: ReactNode }) {
  return (
    <div style={{ minHeight: 36, display: "flex", alignItems: "center", gap: 8, padding: "6px 14px", fontSize: 13, background: warn ? t.alertSoft : t.accentSoft, color: warn ? t.alert : t.textPrimary, flexShrink: 0 }}>
      {children}
    </div>
  )
}
function Body({ children, flush, scroll, style, t: _t }: { children: ReactNode; flush?: boolean; scroll?: boolean; style?: CSSProperties; t?: Tok }) {
  return <div style={{ flex: 1, overflowY: (flush || scroll) ? "auto" : "hidden", padding: flush ? 0 : 14, ...style }}>{children}</div>
}
function Card({ t, style, children, tight, onClick }: { t: Tok; style?: CSSProperties; children: ReactNode; tight?: boolean; onClick?: () => void }) {
  return (
    <div onClick={onClick} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 10, padding: tight ? 0 : 14, overflow: tight ? "hidden" : undefined, marginBottom: 10, ...style }}>
      {children}
    </div>
  )
}
function CovBar({ t, v, r, e, u, style }: { t: Tok; v: number; r: number; e: number; u: number; style?: CSSProperties }) {
  return (
    <div style={{ display: "flex", height: 5, borderRadius: 999, overflow: "hidden", margin: "12px 0 6px", ...style }}>
      <div style={{ width: `${v}%`, background: t.verified }} />
      <div style={{ width: `${r}%`, background: t.repaired }} />
      <div style={{ width: `${e}%`, background: t.escalated }} />
      <div style={{ width: `${u}%`, background: t.uncertain }} />
    </div>
  )
}
function Btn({ t, variant = "pri", small, children, onClick, style }: { t: Tok; variant?: "pri" | "sec" | "txt" | "dan"; small?: boolean; children: ReactNode; onClick?: () => void; style?: CSSProperties }) {
  const styles: Record<string, CSSProperties> = {
    pri: { background: t.accent, color: t.onAccent },
    sec: { border: `1px solid ${t.btnSecBorder}`, color: t.btnSecText },
    txt: { color: t.btnSecText, height: 34 },
    dan: { border: `1px solid ${t.alert}`, color: t.alert },
  }
  return (
    <div onClick={onClick} style={{ height: small ? 36 : 44, borderRadius: 7, display: "flex", alignItems: "center", justifyContent: "center", gap: 6, fontSize: small ? 13 : 15, fontWeight: 500, width: "100%", cursor: "pointer", ...styles[variant], ...style }}>
      {children}
    </div>
  )
}
function Field({ t, label, value, ph, focus, err }: { t: Tok; label: string; value?: string; ph?: string; focus?: boolean; err?: boolean }) {
  const border = err ? `2px solid ${t.alert}` : focus ? `2px solid ${t.accentLine}` : `1px solid ${t.border}`
  const lblColor = err ? t.alert : focus ? t.accentText : t.textSecondary
  return (
    <div style={{ minHeight: 50, background: t.surfaceHigh, border, borderRadius: 7, padding: "7px 12px", display: "flex", flexDirection: "column", justifyContent: "center", marginBottom: 10 }}>
      <div style={{ fontSize: 10, color: lblColor, letterSpacing: ".3px" }}>{label}</div>
      {value
        ? <div style={{ fontSize: 15, marginTop: 1 }}>{value}</div>
        : <div style={{ fontSize: 15, color: t.textSecondary, marginTop: 1 }}>{ph}</div>}
    </div>
  )
}
function Item({ t, children, compact, onClick, style }: { t: Tok; children: ReactNode; compact?: boolean; onClick?: () => void; style?: CSSProperties }) {
  return (
    <div onClick={onClick} style={{ display: "flex", alignItems: "center", gap: 12, padding: compact ? "8px 14px" : "10px 14px", borderBottom: `1px solid ${t.border}`, minHeight: compact ? 48 : 64, cursor: onClick ? "pointer" : "default", ...style }}>
      {children}
    </div>
  )
}
function Thumb({ t, children, style }: { t: Tok; children: ReactNode; style?: CSSProperties }) {
  return (
    <div style={{ width: 42, height: 42, borderRadius: 7, background: t.surfaceHigh, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary, flexShrink: 0, ...style }}>
      {children}
    </div>
  )
}
function Badge({ bg, color, children, style, t: _t }: { bg: string; color: string; children: ReactNode; style?: CSSProperties; t?: Tok }) {
  return <span style={{ fontSize: 10, fontWeight: 600, letterSpacing: ".5px", padding: "2px 6px", borderRadius: 6, background: bg, color, whiteSpace: "nowrap", ...style }}>{children}</span>
}
function Chip({ t, state }: { t: Tok; state: "verified" | "repaired" | "escalated" | "uncertain" }) {
  if (state === "verified") return <svg width="11" height="11"><circle cx="5.5" cy="5.5" r="5.5" fill={t.verified} /></svg>
  if (state === "repaired") return <svg width="11" height="11"><rect width="10" height="10" rx="2" fill={t.repaired} /></svg>
  if (state === "escalated") return <svg width="13" height="11"><polygon points="6.5,0 13,11 0,11" fill={t.escalated} /></svg>
  return <svg width="11" height="11"><circle cx="5.5" cy="5.5" r="4.8" fill="none" stroke={t.uncertain} strokeWidth="1.4" strokeDasharray="2 2" /></svg>
}

// ── Drawer ───────────────────────────────────────────────────────────────────
function Drawer({ t, role, nav }: { t: Tok; role: "student" | "teacher"; nav: Nav }) {
  const isTeacher = role === "teacher"
  const studentItems = [
    { id: "home", label: "Inicio", active: true, screen: "B2" },
    { id: "school", label: "Mis cursos", screen: "C1" },
    { id: "notes", label: "Apuntes", screen: "E1" },
    { id: "coverage", label: "Cobertura", screen: "F1" },
    { id: "study", label: "Estudio", screen: "G1" },
    { id: "sync", label: "Sincronización", screen: "H1" },
    { id: "cost", label: "Consumo IA", screen: "H2" },
  ]
  const teacherItems = [
    { id: "home", label: "Inicio", active: true, screen: "B3" },
    { id: "school", label: "Mis cursos", screen: "I1" },
    { id: "syllabus", label: "Temario", screen: "I3" },
    { id: "glossary", label: "Glosario de notación", screen: "I4" },
    { id: "gaps", label: "Brechas del grupo", screen: "I5" },
    { id: "cost", label: "Consumo del curso", screen: "H2" },
  ]
  const items = isTeacher ? teacherItems : studentItems
  return (
    <div style={{ position: "absolute", inset: 0, background: t.scrim, display: "flex", zIndex: 10 }} onClick={() => nav(isTeacher ? "B3" : "B2")}>
      <div style={{ width: 248, background: t.surface, borderRight: `1px solid ${t.border}`, display: "flex", flexDirection: "column", overflowY: "auto" }} onClick={e => e.stopPropagation()}>
        <div style={{ padding: "16px 14px 14px", borderBottom: `1px solid ${t.border}` }}>
          <div style={{ width: 48, height: 48, borderRadius: 999, background: t.surfaceHigh, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, fontWeight: 600, color: t.accentText }}>
            {isTeacher ? "MG" : "BB"}
          </div>
          <div style={{ fontSize: 15, marginTop: 10 }}>{isTeacher ? "Maikol Guzmán" : "Brandon Brenes"}</div>
          <div style={{ fontSize: 12, color: t.textSecondary }}>{isTeacher ? "maikol.guzman@una.cr" : "bbrenes@est.una.ac.cr"}</div>
          <div style={{ display: "inline-block", marginTop: 8, fontSize: 10, fontWeight: 600, letterSpacing: ".6px", padding: "3px 8px", borderRadius: 999, background: t.accentSoft, color: t.accentText }}>
            {isTeacher ? "DOCENTE" : "ESTUDIANTE"}
          </div>
        </div>
        <div style={{ padding: "10px 14px", borderBottom: `1px solid ${t.border}` }}>
          <div style={{ fontSize: 10, color: t.textSecondary, textTransform: "uppercase", letterSpacing: ".5px" }}>Curso activo</div>
          <div style={{ display: "flex", justifyContent: "space-between", marginTop: 3 }}>
            <span style={{ fontSize: 15 }}>{isTeacher ? "EIF411" : "Cálculo II"}</span>
            <span style={{ color: t.accentText, fontSize: 13 }}>Cambiar</span>
          </div>
        </div>
        <div style={{ padding: "6px 0", flex: 1 }}>
          {items.map(it => (
            <div key={it.id} onClick={() => nav(it.screen)} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, position: "relative", cursor: "pointer", background: it.active ? t.accentSoft : "transparent", color: it.active ? t.accentText : t.textPrimary }}>
              {it.active && <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: t.accent }} />}
              <Ic id={it.id} s={20} style={{ color: it.active ? t.accentText : t.textSecondary }} />
              {it.label}
            </div>
          ))}
          <div style={{ height: 1, background: t.border, margin: "6px 14px" }} />
          <div onClick={() => nav("K2")} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, cursor: "pointer", color: t.textPrimary }}>
            <Ic id="person" s={20} style={{ color: t.textSecondary }} />Perfil
          </div>
          <div onClick={() => nav("K1")} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, cursor: "pointer", color: t.textPrimary }}>
            <Ic id="settings" s={20} style={{ color: t.textSecondary }} />Ajustes
          </div>
          <div onClick={() => nav("A2")} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, cursor: "pointer", color: t.alert }}>
            <Ic id="logout" s={20} style={{ color: t.alert }} />Cerrar sesión
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Course switcher sheet ────────────────────────────────────────────────────
function CourseSwitcher({ t, nav }: { t: Tok; nav: Nav }) {
  const courses = [
    { name: "Cálculo II", code: "MAT-202", pct: "68%", active: true },
    { name: "Física General II", code: "FIS-204", pct: "45%" },
    { name: "Programación IV", code: "EIF-209", pct: "82%" },
  ]
  return (
    <div style={{ position: "absolute", inset: 0, background: t.scrim, display: "flex", flexDirection: "column", justifyContent: "flex-end", zIndex: 10 }} onClick={() => nav("B2")}>
      <div style={{ background: t.surface, borderTop: `1px solid ${t.border}`, borderRadius: "14px 14px 0 0", padding: "6px 0 14px" }} onClick={e => e.stopPropagation()}>
        <div style={{ width: 32, height: 3, borderRadius: 999, background: t.border, margin: "4px auto 12px" }} />
        <div style={{ padding: "0 14px 10px" }}>
          <div style={{ fontSize: 17, fontWeight: 500 }}>Cambiar de curso</div>
          <div style={{ fontSize: 11, color: t.textSecondary, marginTop: 2 }}>Se guarda en el dispositivo.</div>
        </div>
        {courses.map((c, i) => (
          <div key={i} onClick={() => nav("B2")} style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 14px", borderBottom: i < courses.length - 1 ? `1px solid ${t.border}` : "none", minHeight: 60, cursor: "pointer" }}>
            <div style={{ width: 18, height: 18, borderRadius: 999, border: `2px solid ${c.active ? t.accentText : t.textSecondary}`, flexShrink: 0, position: "relative" }}>
              {c.active && <div style={{ position: "absolute", inset: 3, borderRadius: 999, background: t.accentText }} />}
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14 }}>{c.name}</div>
              <div style={{ fontSize: 12, color: t.textSecondary }}>{c.code} · {c.pct} de cobertura</div>
            </div>
          </div>
        ))}
        <div style={{ padding: "10px 14px 0" }}>
          <Btn t={t} variant="sec" small onClick={() => nav("C2")}><Ic id="add" s={16} />Unirme a otro curso</Btn>
        </div>
      </div>
    </div>
  )
}

// ── screens ──────────────────────────────────────────────────────────────────
function ScreenA1({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 18 }}>
        <svg width="88" height="88" viewBox="0 0 88 88">
          <circle cx="44" cy="44" r="39" fill="none" stroke={t.accentText} strokeWidth="1.2" opacity=".45" />
          <path d="M30 58 C30 34, 58 34, 58 48 C58 58, 42 60, 38 50 C35 43, 46 38, 52 44" fill="none" stroke={t.accentText} strokeWidth="3.4" strokeLinecap="round" />
          <circle cx="56.5" cy="29.5" r="2.8" fill={t.accentText} />
        </svg>
        <div style={{ fontSize: 28, fontWeight: 600, letterSpacing: "-.5px" }}>Glifo</div>
        <div style={{ fontSize: 13, color: t.textSecondary }}>El glifo es la unidad de la escritura.</div>
      </div>
      <div style={{ padding: "0 14px 20px", display: "flex", flexDirection: "column", gap: 8 }}>
        <Btn t={t} onClick={() => nav("A2")}>Entrar</Btn>
        <Btn t={t} variant="sec" onClick={() => nav("A3")}>Crear cuenta</Btn>
        <div style={{ textAlign: "center", fontSize: 11, color: t.textSecondary, marginTop: 4 }}>Grupo X-Ray · EIF411</div>
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenA2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <Body>
        <div style={{ height: 24 }} />
        <svg width="44" height="44" viewBox="0 0 88 88">
          <path d="M30 58 C30 34, 58 34, 58 48 C58 58, 42 60, 38 50 C35 43, 46 38, 52 44" fill="none" stroke={t.accentText} strokeWidth="4.2" strokeLinecap="round" />
        </svg>
        <div style={{ fontSize: 20, fontWeight: 500, margin: "18px 0 5px" }}>Entrar a Glifo</div>
        <div style={{ fontSize: 13, color: t.textSecondary, marginBottom: 22 }}>Usá tu correo institucional.</div>
        <Field t={t} label="Correo" value="bbrenes@est.una.ac.cr" focus />
        <Field t={t} label="Contraseña" ph="••••••••" />
        <div style={{ height: 8 }} />
        <Btn t={t} onClick={() => nav("B2")}>Entrar como Estudiante</Btn>
        <div style={{ height: 6 }} />
        <Btn t={t} variant="sec" onClick={() => nav("B3")}>Entrar como Docente</Btn>
        <div style={{ height: 6 }} />
        <Btn t={t} variant="sec" onClick={() => nav("B4")}>Entrar como Admin</Btn>
        <div style={{ height: 6 }} />
        <Btn t={t} variant="txt" onClick={() => nav("A4")}>Olvidé mi contraseña</Btn>
        <div style={{ flex: 1, minHeight: 40 }} />
        <div style={{ display: "flex", justifyContent: "space-between", paddingTop: 8, marginTop: 24 }}>
          <span style={{ fontSize: 13, color: t.textSecondary }}>¿Todavía no tenés cuenta?</span>
          <span onClick={() => nav("A3")} style={{ fontSize: 14, fontWeight: 500, color: t.accentText, cursor: "pointer" }}>Crear cuenta</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenA4({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("A2")}><Ic id="back" s={22} /></Tap>} title="Recuperar contraseña" />
      <Body>
        <div style={{ height: 16 }} />
        <div style={{ fontSize: 14, color: t.textSecondary, marginBottom: 24, lineHeight: 1.55 }}>
          Ingresá tu correo institucional y te enviaremos un enlace para restablecer tu contraseña.
        </div>
        <Field t={t} label="Correo institucional" value="bbrenes@est.una.ac.cr" focus />
        <div style={{ height: 8 }} />
        <Btn t={t} onClick={() => nav("A2")}>Enviar enlace</Btn>
        <div style={{ height: 14 }} />
        <Banner t={t}><Ic id="info" s={16} /><span style={{ fontSize: 12 }}>Si la dirección está registrada, recibirás el correo en menos de 5 minutos. Revisá también la carpeta de spam.</span></Banner>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenA3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("A2")}><Ic id="back" s={22} /></Tap>} title="Crear cuenta" />
      <Body>
        <Field t={t} label="Nombre completo" value="Brandon Brenes" />
        <Field t={t} label="Correo" value="bbrenes@est.una.ac.cr" />
        <Field t={t} label="Contraseña" ph="Mínimo 8 caracteres" />
        <Field t={t} label="Confirmar contraseña" ph="••••••" err />
        <div style={{ display: "flex", alignItems: "center", gap: 6, color: t.alert, margin: "-4px 0 14px", fontSize: 12 }}>
          <Ic id="warn" s={16} />Las dos contraseñas deben coincidir.
        </div>
        <Btn t={t} onClick={() => nav("B2")}>Crear cuenta</Btn>
        <div style={{ height: 14 }} />
        <div style={{ fontSize: 13, color: t.textSecondary, lineHeight: 1.55 }}>Vas a entrar como estudiante. Si sos docente, pedile el cambio de rol al administrador.</div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenB1a({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <div style={{ flex: 1, position: "relative", overflow: "hidden" }}>
        <Drawer t={t} role="student" nav={nav} />
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenB1b({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <div style={{ flex: 1, position: "relative", overflow: "hidden" }}>
        <Drawer t={t} role="teacher" nav={nav} />
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenB1c({ t, nav }: { t: Tok; nav: Nav }) {
  const adminItems = [
    { id: "home", label: "Inicio", active: true, screen: "B4" },
    { id: "users", label: "Usuarios", screen: "J1" },
    { id: "school", label: "Cursos", screen: "I1" },
    { id: "roles", label: "Roles", screen: "J3" },
    { id: "cost", label: "Consumo IA", screen: "H2" },
  ]
  return (
    <>
      <SysBar t={t} />
      <div style={{ flex: 1, position: "relative", overflow: "hidden" }}>
        <div style={{ position: "absolute", inset: 0, background: t.scrim, display: "flex", zIndex: 10 }} onClick={() => nav("B4")}>
          <div style={{ width: 248, background: t.surface, borderRight: `1px solid ${t.border}`, display: "flex", flexDirection: "column", overflowY: "auto" }} onClick={e => e.stopPropagation()}>
            <div style={{ padding: "16px 14px 14px", borderBottom: `1px solid ${t.border}` }}>
              <div style={{ width: 48, height: 48, borderRadius: 999, background: t.accentSoft, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, fontWeight: 600, color: t.accentText }}>AD</div>
              <div style={{ fontSize: 15, marginTop: 10 }}>Admin Glifo</div>
              <div style={{ fontSize: 12, color: t.textSecondary }}>admin@glifo.app</div>
              <div style={{ display: "inline-block", marginTop: 8, fontSize: 10, fontWeight: 600, letterSpacing: ".6px", padding: "3px 8px", borderRadius: 999, background: t.alertSoft, color: t.alert }}>ADMIN</div>
            </div>
            <div style={{ padding: "6px 0", flex: 1 }}>
              {adminItems.map(it => (
                <div key={it.id} onClick={() => nav(it.screen)} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, position: "relative", cursor: "pointer", background: it.active ? t.accentSoft : "transparent", color: it.active ? t.accentText : t.textPrimary }}>
                  {it.active && <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: t.accent }} />}
                  <Ic id={it.id} s={20} style={{ color: it.active ? t.accentText : t.textSecondary }} />
                  {it.label}
                </div>
              ))}
              <div style={{ height: 1, background: t.border, margin: "6px 14px" }} />
              <div onClick={() => nav("K1")} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, cursor: "pointer", color: t.textPrimary }}>
                <Ic id="settings" s={20} style={{ color: t.textSecondary }} />Ajustes
              </div>
              <div onClick={() => nav("A2")} style={{ height: 44, display: "flex", alignItems: "center", gap: 14, padding: "0 14px", fontSize: 14, cursor: "pointer", color: t.alert }}>
                <Ic id="logout" s={20} style={{ color: t.alert }} />Cerrar sesión
              </div>
            </div>
          </div>
        </div>
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenB2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t}
        left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>}
        title="Cálculo II" sub="Curso activo"
        right={<Tap t={t} onClick={() => nav("F3")}><Ic id="clock" s={22} /></Tap>}
      />
      <Banner t={t}><Ic id="sync" s={16} />2 capturas en cola · se envían al volver la red</Banner>
      <Body>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <span style={{ fontSize: 16, fontWeight: 500 }}>Cobertura del curso</span>
            <span style={{ fontSize: 22, fontWeight: 600, color: t.accentText }}>68%</span>
          </div>
          <CovBar t={t} v={54} r={20} e={14} u={12} />
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <span style={{ fontSize: 12, color: t.textSecondary }}>18 sólidos · 7 parciales · 5 ausentes</span>
            <span style={{ fontSize: 12, fontWeight: 500, color: t.verified }}>+2 desde el 8 ago</span>
          </div>
        </Card>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Pendientes de hoy</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 10 }}>
          {[
            { icon: "study", n: "12 flashcards", m: "Series · Integrales impropias", screen: "G2" },
            { icon: "coverage", n: "1 quiz", m: "De los vacíos detectados", screen: "G3" },
          ].map(q => (
            <div key={q.n} onClick={() => nav(q.screen)} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 10, padding: 12, minHeight: 84, display: "flex", flexDirection: "column", justifyContent: "space-between", cursor: "pointer" }}>
              <span style={{ fontSize: 14, fontWeight: 500, display: "flex", alignItems: "center", gap: 6 }}>
                <Ic id={q.icon} s={18} style={{ color: t.accentText }} />{q.n}
              </span>
              <span style={{ fontSize: 11, color: t.textSecondary, lineHeight: 1.35 }}>{q.m}</span>
            </div>
          ))}
        </div>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Últimos apuntes</div>
        <Card t={t} style={{ padding: 0, overflow: "hidden" }}>
          {[
            { title: "Integrales impropias", meta: "12 ago · 3 páginas", chips: ["verified","repaired","escalated"] as const },
            { title: "Criterio de comparación", meta: "7 ago · 2 páginas", chips: ["verified","uncertain"] as const },
          ].map((it, i) => (
            <Item key={i} t={t} onClick={() => nav("E2")}>
              <Thumb t={t}><Ic id="notes" s={18} /></Thumb>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14 }}>{it.title}</div>
                <div style={{ fontSize: 12, color: t.textSecondary }}>{it.meta}</div>
              </div>
              <div style={{ display: "flex", gap: 4 }}>{it.chips.map(c => <Chip key={c} t={t} state={c} />)}</div>
            </Item>
          ))}
        </Card>
      </Body>
      <div onClick={() => nav("D1")} style={{ position: "absolute", right: 14, bottom: 36, width: 50, height: 50, borderRadius: 16, background: t.accent, color: t.onAccent, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", zIndex: 4 }}>
        <Ic id="camera" s={22} />
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenB3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t}
        left={<Tap t={t} onClick={() => nav("B1b")}><Ic id="menu" s={22} /></Tap>}
        title="EIF411" sub="Curso activo"
      />
      <Body>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <span style={{ fontSize: 16, fontWeight: 500 }}>Cobertura del grupo</span>
            <span style={{ fontSize: 22, fontWeight: 600, color: t.accentText }}>54%</span>
          </div>
          <CovBar t={t} v={40} r={24} e={26} u={10} />
          <div style={{ fontSize: 12, color: t.textSecondary }}>27 estudiantes inscritos · promedio del curso</div>
        </Card>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Temas con más vacíos</div>
        <Card t={t} style={{ padding: 0, overflow: "hidden" }}>
          {[
            { chip: "escalated", title: "4.3 Sockets e hilos", meta: "19 de 27 sin cubrir", pct: "70%" },
            { chip: "uncertain", title: "6.1 Seguridad y permisos", meta: "14 de 27 sin cubrir", pct: "52%" },
            { chip: "repaired", title: "3.2 Jetpack Compose", meta: "8 de 27 sin cubrir", pct: "30%" },
          ].map((it, i) => (
            <Item key={i} t={t} compact onClick={() => nav("I5")}>
              <Chip t={t} state={it.chip as any} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13 }}>{it.title}</div>
                <div style={{ fontSize: 11, color: t.textSecondary }}>{it.meta}</div>
              </div>
              <span style={{ fontSize: 12, color: t.textSecondary }}>{it.pct}</span>
              <Ic id="right" s={16} style={{ color: t.textSecondary }} />
            </Item>
          ))}
        </Card>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Requiere atención</div>
        <Card t={t} style={{ borderColor: t.alertLine, background: t.alertFaint }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Ic id="warn" s={18} style={{ color: t.alert }} />
            <span style={{ fontSize: 15, fontWeight: 500 }}>Álgebra Lineal no tiene temario</span>
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, margin: "6px 0 12px", lineHeight: 1.5 }}>Sin temario no se puede calcular la cobertura.</div>
          <Btn t={t} variant="sec" small onClick={() => nav("I3")}>Publicar temario</Btn>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenC1({ t, nav }: { t: Tok; nav: Nav }) {
  const courses = [
    { name: "Cálculo II", code: "MAT-202 · Prof. Rodríguez", v: 54, r: 20, e: 14, u: 12, pct: "68%", delta: "+2", active: true },
    { name: "Física General II", code: "FIS-204 · Prof. Mora", v: 31, r: 14, e: 25, u: 30, pct: "45%", delta: "" },
    { name: "Programación IV", code: "EIF-209 · Prof. Guzmán", v: 70, r: 12, e: 10, u: 8, pct: "82%", delta: "+5" },
  ]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>} title="Mis cursos" right={<Tap t={t}><Ic id="search" s={22} /></Tap>} />
      <Body>
        {courses.map(c => (
          <Card t={t} key={c.name} style={{ cursor: "pointer" }} onClick={() => nav("C3")}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ fontSize: 16, fontWeight: 500 }}>{c.name}</span>
              {c.active && <Badge bg={t.accentSoft} color={t.accentText}>ACTIVO</Badge>}
            </div>
            <div style={{ fontFamily: "ui-monospace,'JetBrains Mono',monospace", fontSize: 11, color: t.textSecondary, marginTop: 2 }}>{c.code}</div>
            <CovBar t={t} v={c.v} r={c.r} e={c.e} u={c.u} />
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ fontSize: 12, color: t.textSecondary }}>{c.pct} de cobertura</span>
              {c.delta ? <span style={{ fontSize: 12, fontWeight: 500, color: t.verified }}>{c.delta}</span> : <span style={{ fontSize: 12, color: t.textSecondary }}>sin cambios</span>}
            </div>
          </Card>
        ))}
        <Btn t={t} variant="sec" onClick={() => nav("C2")}><Ic id="add" s={18} />Unirme con un código</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenC2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("C1")}><Ic id="back" s={22} /></Tap>} title="Unirme a un curso" />
      <Body>
        <div style={{ fontSize: 13, color: t.textSecondary, lineHeight: 1.55, marginBottom: 16 }}>Pedile el código a tu profesor. Son ocho caracteres y no distingue mayúsculas.</div>
        <Field t={t} label="Código del curso" value="GLF-4K2P" focus />
        <Btn t={t}>Buscar curso</Btn>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Curso encontrado</div>
        <Card t={t} style={{ borderColor: t.accentLine }}>
          <div style={{ fontSize: 16, fontWeight: 500 }}>EIF411 — Plataformas Móviles</div>
          <div style={{ fontFamily: "ui-monospace,monospace", fontSize: 11, color: t.textSecondary, marginTop: 2 }}>II Ciclo 2026 · Prof. Maikol Guzmán</div>
          <div style={{ display: "flex", gap: 14, marginTop: 12 }}>
            <span style={{ fontSize: 12, color: t.textSecondary, display: "flex", alignItems: "center", gap: 5 }}><Ic id="users" s={14} />27 inscritos</span>
            <span style={{ fontSize: 12, color: t.textSecondary, display: "flex", alignItems: "center", gap: 5 }}><Ic id="syllabus" s={14} />33 temas</span>
          </div>
          <div style={{ height: 12 }} />
          <Btn t={t} small onClick={() => nav("B2")}>Unirme a este curso</Btn>
        </Card>
        <div style={{ display: "flex", gap: 8, marginTop: 4, alignItems: "flex-start" }}>
          <Ic id="warn" s={14} style={{ color: t.textSecondary, marginTop: 1 }} />
          <span style={{ fontSize: 11, color: t.textSecondary, lineHeight: 1.5 }}>Tus apuntes son privados. El profesor ve la cobertura del grupo, nunca tus fotos.</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenC2b({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B2")}><Ic id="menu" s={22} /></Tap>} title="Cálculo II" sub="Curso activo" />
      <div style={{ flex: 1, position: "relative" }}>
        <CourseSwitcher t={t} nav={nav} />
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenD1({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <div style={{ flex: 1, background: `radial-gradient(120% 80% at 50% 45%, ${t.surfaceHigh} 0%, ${t.surface} 55%, ${t.background} 100%)`, position: "relative", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 50, display: "flex", alignItems: "center", padding: "0 10px", gap: 8, zIndex: 3 }}>
          <div onClick={() => nav("B2")} style={{ color: t.textPrimary, cursor: "pointer" }}><Ic id="close" s={22} /></div>
          <div style={{ flex: 1, textAlign: "center", fontSize: 12, color: t.textPrimary }}>Cálculo II · página 1</div>
          <div style={{ color: t.textPrimary }}><Ic id="flash" s={22} /></div>
        </div>
        <svg width="220" height="290" viewBox="0 0 252 330" style={{ opacity: .95 }}>
          <path d="M18 26 L228 14 L240 300 L28 314 Z" fill={t.accentFaint} stroke={t.accentText} strokeWidth="2" />
          <g stroke={t.accentText} strokeWidth="4" strokeLinecap="round" fill="none">
            <path d="M18 56 L18 26 L48 24" /><path d="M198 16 L228 14 L229 44" />
            <path d="M239 270 L240 300 L210 302" /><path d="M58 312 L28 314 L27 284" />
          </g>
          <g stroke={t.textSecondary} strokeWidth="1.4" opacity=".5">
            <path d="M40 80 H210 M40 104 H196 M40 128 H214" />
            <path d="M40 176 H130 M40 200 H180 M40 224 H150" />
          </g>
          <g stroke={t.accentText} strokeWidth="1.8" opacity=".75" fill="none">
            <path d="M96 148 c8 -12 20 -12 26 0 c6 12 18 12 26 0" /><path d="M92 152 h64" />
          </g>
        </svg>
        <div style={{ position: "absolute", bottom: 108, left: 0, right: 0, textAlign: "center" }}>
          <span style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 999, padding: "5px 12px", fontSize: 11, color: t.accentText }}>Página detectada · mantené el pulso</span>
        </div>
      </div>
      <div style={{ height: 100, background: t.background, display: "flex", alignItems: "center", justifyContent: "space-around", padding: "0 20px", flexShrink: 0 }}>
        <div style={{ width: 40, height: 40, borderRadius: 7, border: `1px solid ${t.border}`, background: t.surfaceHigh, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary }}>
          <Ic id="image" s={18} />
        </div>
        <div style={{ width: 60, height: 60, borderRadius: 999, border: `3px solid ${t.textPrimary}`, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }} onClick={() => nav("D2")}>
          <div style={{ width: 48, height: 48, borderRadius: 999, background: t.textPrimary }} />
        </div>
        <div style={{ width: 40, height: 40, borderRadius: 7, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 600, color: t.textSecondary }}>1/8</div>
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenD2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("D1")}><Ic id="close" s={22} /></Tap>} title="Revisá la foto" />
      <Body>
        <div style={{ height: 170, marginBottom: 12, background: `repeating-linear-gradient(45deg,${t.surfaceHigh},${t.surfaceHigh} 6px,${t.border} 6px,${t.border} 12px)`, border: `1px solid ${t.border}`, borderRadius: 7, filter: "blur(1.4px)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <span style={{ filter: "none", fontSize: 11, color: t.textSecondary }}>Vista previa de la captura</span>
        </div>
        <Card t={t} style={{ borderColor: t.alertLine, background: t.alertFaint }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Ic id="warn" s={18} style={{ color: t.alert }} />
            <span style={{ fontSize: 16, fontWeight: 500 }}>La foto está movida</span>
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 6, lineHeight: 1.5 }}>Con este nivel de desenfoque el reconocimiento inventaría texto. Apoyá el codo en la mesa y repetí la toma.</div>
        </Card>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "14px 0 8px" }}>Diagnóstico de calidad</div>
        <Card t={t}>
          {[
            { icon: "warn", color: t.alert, label: "Nitidez", pct: 24, score: "12 / 35" },
            { icon: "check", color: t.verified, label: "Iluminación", pct: 78, score: "78" },
            { icon: "check", color: t.verified, label: "Sin reflejos", pct: 91, score: "91" },
            { icon: "check", color: t.verified, label: "Perspectiva", pct: 100, score: "corregida −4.2°" },
          ].map(row => (
            <div key={row.label} style={{ display: "flex", alignItems: "center", gap: 8, padding: "7px 0", borderBottom: `1px solid ${t.border}`, fontSize: 13 }}>
              <Ic id={row.icon} s={16} style={{ color: row.color }} />
              <span style={{ flex: 1 }}>{row.label}</span>
              <div style={{ width: 64, height: 5, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden" }}>
                <div style={{ width: `${row.pct}%`, height: "100%", background: row.color }} />
              </div>
              <span style={{ fontSize: 11, fontFamily: "ui-monospace,monospace", color: t.textSecondary }}>{row.score}</span>
            </div>
          ))}
        </Card>
        <Btn t={t} onClick={() => nav("D1")}><Ic id="camera" s={18} />Repetir la foto</Btn>
        <div style={{ height: 6 }} />
        <Btn t={t} variant="txt" onClick={() => nav("D3")}>Procesarla igual, sé que va a fallar</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenD3({ t, nav }: { t: Tok; nav: Nav }) {
  const steps = [
    { state: "done", n: "N0 · Preprocesamiento", d: "Perspectiva −4.2° · contraste normalizado · 18 regiones segmentadas" },
    { state: "done", n: "N1 · OCR de texto", d: "18 de 18 regiones leídas · ML Kit, en el dispositivo · 1.1 s" },
    { state: "done", n: "Clasificador de región", d: "14 texto · 3 fórmulas · 1 dibujo" },
    { state: "now", n: "N1.5 · OCR matemático", d: "2 de 3 fórmulas convertidas a LaTeX · SimpleTex · capa gratuita" },
    { state: "pending", n: "N2 · Reparación por visión", d: "Se activa solo si algo no supera las compuertas." },
  ]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t}
        left={<Tap t={t} onClick={() => nav("B2")}><Ic id="close" s={22} /></Tap>}
        title="Procesando"
        right={<div style={{ marginRight: 8 }}><Badge bg={t.verifiedSoft} color={t.verified}>0 LLAMADAS DE PAGO</Badge></div>}
      />
      <Body>
        <div style={{ display: "flex", gap: 12, marginBottom: 18 }}>
          <div style={{ width: 56, height: 74, flexShrink: 0, background: `repeating-linear-gradient(45deg,${t.surfaceHigh},${t.surfaceHigh} 6px,${t.border} 6px,${t.border} 12px)`, border: `1px solid ${t.border}`, borderRadius: 6 }} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 500 }}>Página 1 de 3</div>
            <div style={{ fontSize: 11, color: t.textSecondary, marginTop: 2 }}>18 regiones detectadas</div>
            <div style={{ height: 5, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", marginTop: 10 }}>
              <div style={{ width: "72%", height: "100%", background: t.accent }} />
            </div>
          </div>
        </div>
        {steps.map((s, i) => (
          <div key={i} style={{ display: "flex", gap: 12, marginBottom: 2 }}>
            <div style={{ width: 22, display: "flex", flexDirection: "column", alignItems: "center", flexShrink: 0 }}>
              <div style={{ width: 22, height: 22, borderRadius: 999, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, fontSize: 10, fontWeight: 600, background: s.state === "done" ? t.verified : "transparent", borderColor: s.state === "done" ? t.verified : s.state === "now" ? t.accent : t.border, borderWidth: 2, borderStyle: "solid", color: s.state === "done" ? t.background : s.state === "now" ? t.accentText : t.textSecondary }}>
                {s.state === "done" ? <Ic id="check" s={13} /> : i + 1}
              </div>
              {i < steps.length - 1 && <div style={{ flex: 1, width: 2, background: s.state === "done" ? t.verified : t.border, margin: "2px 0", minHeight: 12 }} />}
            </div>
            <div style={{ paddingBottom: 14, flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 500, color: s.state === "now" ? t.accentText : s.state === "pending" ? t.textSecondary : t.textPrimary }}>{s.n}</div>
              <div style={{ fontSize: 11, color: t.textSecondary, marginTop: 2, lineHeight: 1.4 }}>{s.d}</div>
            </div>
          </div>
        ))}
        <div style={{ height: 10 }} />
        <Btn t={t} onClick={() => nav("E2")}>Ver resultados</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenE1({ t, nav }: { t: Tok; nav: Nav }) {
  const notes = [
    { title: "Integrales impropias", meta: "12 ago · 3 páginas", chips: ["verified", "repaired", "escalated"] as const },
    { title: "Criterio de comparación", meta: "7 ago · 2 páginas", chips: ["verified", "uncertain"] as const },
    { title: "Series de potencias", meta: "2 ago · 4 páginas", chips: ["verified", "verified", "repaired"] as const },
    { title: "Prueba de la integral", meta: "28 jul · 2 páginas", chips: ["verified"] as const },
  ]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>} title="Apuntes" right={<Tap t={t}><Ic id="search" s={22} /></Tap>} />
      <Banner t={t}><Ic id="sync" s={16} />2 capturas en cola</Banner>
      <Body flush>
        {notes.map((n, i) => (
          <div key={i} onClick={() => nav("E2")} style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 14px", borderBottom: `1px solid ${t.border}`, minHeight: 64, cursor: "pointer" }}>
            <Thumb t={t}><Ic id="notes" s={18} /></Thumb>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14 }}>{n.title}</div>
              <div style={{ fontSize: 12, color: t.textSecondary }}>{n.meta}</div>
            </div>
            <div style={{ display: "flex", gap: 4 }}>{n.chips.map((c, ci) => <Chip key={ci} t={t} state={c} />)}</div>
          </div>
        ))}
      </Body>
      <div onClick={() => nav("D1")} style={{ position: "absolute", right: 14, bottom: 36, width: 50, height: 50, borderRadius: 16, background: t.accent, color: t.onAccent, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", zIndex: 4 }}>
        <Ic id="camera" s={22} />
      </div>
      <GestureBar t={t} />
    </>
  )
}

function ScreenE2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t}
        left={<Tap t={t} onClick={() => nav("E1")}><Ic id="back" s={22} /></Tap>}
        title="Integrales impropias"
        right={<><Tap t={t} onClick={() => nav("E5")}><Ic id="image" s={22} /></Tap><Tap t={t} onClick={() => nav("E3")}><Ic id="edit" s={22} /></Tap></>}
      />
      <Banner t={t}>
        <div style={{ display: "flex", gap: 4 }}>
          <Chip t={t} state="verified" /><Chip t={t} state="repaired" /><Chip t={t} state="escalated" /><Chip t={t} state="uncertain" />
        </div>
        <span style={{ fontSize: 12 }}>14 verificados · 2 reparados · 1 escalado · 1 incierto</span>
      </Banner>
      <Body>
        <div style={{ fontSize: 11, color: t.textSecondary, marginBottom: 10 }}>Clase del 12 de agosto · página 1 de 3</div>
        <div style={{ fontSize: 17, fontWeight: 500, marginBottom: 8 }}>
          <span style={{ borderBottom: `2px solid ${t.verified}`, borderRadius: 2, paddingBottom: 1 }}>Integrales impropias de primera especie</span>
        </div>
        <p style={{ fontSize: 14, lineHeight: 1.75, marginBottom: 12 }}>
          <span style={{ borderBottom: `2px solid ${t.verified}`, borderRadius: 2 }}>Una integral es impropia cuando el intervalo de integración no está acotado, o cuando la función presenta una </span>
          <span onClick={() => nav("E5")} style={{ borderBottom: `2px solid ${t.escalated}`, background: t.escalatedFaint, borderRadius: 2, cursor: "pointer" }}>discontinuidad infinita</span>
          <span style={{ borderBottom: `2px solid ${t.verified}`, borderRadius: 2 }}> dentro del intervalo.</span>
        </p>
        <div onClick={() => nav("E4")} style={{ background: t.surfaceHigh, border: `1px solid ${t.border}`, borderLeft: `3px solid ${t.repaired}`, borderRadius: 7, padding: 12, marginBottom: 12, cursor: "pointer" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
            <Badge bg={t.repairedSoft} color={t.repaired}>REPARADO · N1.5</Badge>
            <span style={{ fontSize: 11, color: t.textSecondary }}>confianza 0.86</span>
          </div>
          <div style={{ textAlign: "center", fontSize: 18, padding: "6px 0" }}>
            ∫<sub style={{ fontSize: 9 }}>a</sub><sup style={{ fontSize: 9 }}>∞</sup> f(x) dx = lim<sub style={{ fontSize: 9 }}>b→∞</sub> ∫<sub style={{ fontSize: 9 }}>a</sub><sup style={{ fontSize: 9 }}>b</sup> f(x) dx
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 8 }}>
            <Ic id="image" s={15} style={{ color: t.textSecondary }} />
            <span style={{ fontSize: 11, color: t.textSecondary }}>Ver el recorte original</span>
          </div>
        </div>
        <p style={{ fontSize: 14, lineHeight: 1.75 }}>
          <span style={{ borderBottom: `2px solid ${t.verified}`, borderRadius: 2 }}>Si el límite existe y es finito, la integral </span>
          <span style={{ borderBottom: `2px dashed ${t.uncertain}`, background: t.uncertainSoft, borderRadius: 2 }}>converge; en caso contrario diverge</span>
          <span style={{ borderBottom: `2px solid ${t.verified}`, borderRadius: 2 }}>.</span>
        </p>
        <Card t={t} onClick={() => nav("E3")} style={{ marginTop: 14, borderColor: t.uncertainLine, background: t.uncertainFaint, cursor: "pointer" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Chip t={t} state="uncertain" />
            <span style={{ fontSize: 13, flex: 1 }}>1 fragmento sin leer con certeza</span>
            <Ic id="right" s={16} style={{ color: t.textSecondary }} />
          </div>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenF1({ t, nav }: { t: Tok; nav: Nav }) {
  const topics = [
    { chip: "verified", name: "3.1 Sucesiones", score: "0.92" },
    { chip: "verified", name: "3.2 Series geométricas", score: "0.88" },
    { chip: "repaired", name: "3.3 Comparación", sub: "Falta el caso del límite", score: "0.54" },
    { chip: "escalated", name: "3.4 Series de potencias", sub: "Sin evidencia en tus apuntes", score: "0.11" },
    { chip: "uncertain", name: "3.5 Radio de convergencia", sub: "Apunte ilegible en esa sección", score: "—" },
  ]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>} title="Cálculo II" sub="Cobertura de" right={<Tap t={t}><Ic id="calendar" s={22} /></Tap>} />
      <Banner t={t}>
        <Ic id="gaps" s={16} />
        <span style={{ flex: 1, fontSize: 12 }}>+2 temas desde el 8 de agosto</span>
        <Ic id="right" s={16} />
      </Banner>
      <Body>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 2 }}>
            <span style={{ fontSize: 15, fontWeight: 500 }}>33 temas</span>
            <span style={{ fontSize: 22, fontWeight: 600, color: t.accentText }}>68%</span>
          </div>
          <div style={{ display: "flex", height: 10, borderRadius: 999, overflow: "hidden", margin: "10px 0 10px" }}>
            <div style={{ width: "54%", background: t.verified }} />
            <div style={{ width: "20%", background: t.repaired }} />
            <div style={{ width: "14%", background: t.escalated }} />
            <div style={{ width: "12%", background: t.uncertain }} />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px 10px" }}>
            {[
              { state: "verified", label: "18 sólidos" },
              { state: "repaired", label: "7 parciales" },
              { state: "escalated", label: "5 ausentes" },
              { state: "uncertain", label: "3 dudosos" },
            ].map(r => (
              <span key={r.label} style={{ fontSize: 12, color: t.textSecondary, display: "flex", alignItems: "center", gap: 5 }}>
                <Chip t={t} state={r.state as any} />{r.label}
              </span>
            ))}
          </div>
        </Card>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "14px 0 8px" }}>Unidad 3 · Series</div>
        <Card t={t} style={{ padding: 0, overflow: "hidden" }}>
          {topics.map((tp, i) => (
            <div key={i} onClick={() => nav("F2")} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 12px", borderBottom: i < topics.length - 1 ? `1px solid ${t.border}` : "none", minHeight: 48, cursor: "pointer" }}>
              <Chip t={t} state={tp.chip as any} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13 }}>{tp.name}</div>
                {tp.sub && <div style={{ fontSize: 11, color: t.textSecondary }}>{tp.sub}</div>}
              </div>
              <span style={{ fontSize: 11, fontFamily: "ui-monospace,monospace", color: t.textSecondary }}>{tp.score}</span>
              <Ic id="right" s={15} style={{ color: t.textSecondary }} />
            </div>
          ))}
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenI2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("I1")}><Ic id="back" s={22} /></Tap>} title="Nuevo curso" />
      <Body>
        <Field t={t} label="Nombre del curso" value="Plataformas Móviles" />
        <Field t={t} label="Código institucional" value="EIF411" />
        <Field t={t} label="Ciclo" value="II Ciclo 2026" />
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "14px 0 8px" }}>Código de inscripción</div>
        <Card t={t} style={{ borderColor: t.accentLine }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontFamily: "ui-monospace,'JetBrains Mono',monospace", fontSize: 24, color: t.accentText, letterSpacing: 3 }}>GLF-4K2P</span>
            <div style={{ display: "flex", gap: 12 }}>
              <Ic id="refresh" s={18} style={{ color: t.textSecondary }} />
              <Ic id="copy" s={18} style={{ color: t.accentText }} />
            </div>
          </div>
          <div style={{ fontSize: 11, color: t.textSecondary, marginTop: 8, lineHeight: 1.5 }}>Los estudiantes lo escriben a mano, por eso no lleva letras ambiguas.</div>
        </Card>
        <Card t={t} style={{ background: t.accentFaint, borderColor: t.accentLine }}>
          <div style={{ display: "flex", gap: 8, alignItems: "flex-start" }}>
            <Ic id="syllabus" s={18} style={{ color: t.accentText }} />
            <div>
              <div style={{ fontSize: 13, fontWeight: 500 }}>El temario va después</div>
              <div style={{ fontSize: 11, color: t.textSecondary, marginTop: 3, lineHeight: 1.5 }}>Sin temario el curso funciona, pero no calcula cobertura.</div>
            </div>
          </div>
        </Card>
        <Btn t={t} onClick={() => nav("B3")}>Crear curso</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenI1({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1b")}><Ic id="menu" s={22} /></Tap>} title="Mis cursos" right={<Tap t={t} onClick={() => nav("I2")}><Ic id="add" s={22} /></Tap>} />
      <Body>
        {[
          { name: "Plataformas Móviles", code: "EIF411 · II Ciclo 2026", students: 27, v: 40, r: 24, e: 26, u: 10, pct: "54%", active: true },
          { name: "Álgebra Lineal", code: "MAT-101 · II Ciclo 2026", students: 34, v: 0, r: 0, e: 0, u: 0, pct: "—", noSyllabus: true },
        ].map(c => (
          <Card t={t} key={c.name} onClick={() => nav("I3")} style={c.noSyllabus ? { borderColor: t.alertLine, cursor: "pointer" } : { cursor: "pointer" }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ fontSize: 15, fontWeight: 500 }}>{c.name}</span>
              {c.active && <Badge bg={t.accentSoft} color={t.accentText}>ACTIVO</Badge>}
            </div>
            <div style={{ fontFamily: "ui-monospace,monospace", fontSize: 11, color: t.textSecondary, marginTop: 2 }}>{c.code}</div>
            {c.noSyllabus
              ? <div style={{ marginTop: 10, padding: "8px 10px", background: t.alertFaint, borderRadius: 6, fontSize: 12, color: t.alert, display: "flex", alignItems: "center", gap: 6 }}>
                  <Ic id="warn" s={14} />Sin temario · sin cobertura
                </div>
              : <>
                  <CovBar t={t} v={c.v} r={c.r} e={c.e} u={c.u} />
                  <div style={{ fontSize: 12, color: t.textSecondary }}>{c.pct} de cobertura · {c.students} estudiantes</div>
                </>}
          </Card>
        ))}
        <Btn t={t} variant="sec" onClick={() => nav("I2")}><Ic id="add" s={18} />Crear nuevo curso</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenK2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav(roleHome === "B4" ? "B1c" : roleHome === "B3" ? "B1b" : "B1a")}><Ic id="menu" s={22} /></Tap>} title="Perfil" />
      <Body>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "6px 0 18px" }}>
          <div style={{ width: 72, height: 72, borderRadius: 999, background: t.surfaceHigh, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24, fontWeight: 600, color: t.accentText }}>BB</div>
          <div style={{ fontSize: 20, fontWeight: 500, marginTop: 12 }}>Brandon Brenes</div>
          <div style={{ fontSize: 13, color: t.textSecondary }}>bbrenes@est.una.ac.cr</div>
          <Badge bg={t.accentSoft} color={t.accentText}>ESTUDIANTE</Badge>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 10 }}>
          <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 10, padding: 12, minHeight: 72 }}>
            <div style={{ fontSize: 20, fontWeight: 500 }}>47</div>
            <div style={{ fontSize: 11, color: t.textSecondary, lineHeight: 1.35 }}>Páginas capturadas</div>
          </div>
          <div style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 10, padding: 12, minHeight: 72 }}>
            <div style={{ fontSize: 20, fontWeight: 500, color: t.verified }}>81%</div>
            <div style={{ fontSize: 11, color: t.textSecondary, lineHeight: 1.35 }}>Resueltas sin llamadas de pago</div>
          </div>
        </div>
        <div style={{ fontSize: 10, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "14px 0 8px" }}>Cuenta</div>
        <Card t={t} style={{ padding: 0, overflow: "hidden" }}>
          {([
            { icon: "shield", label: "Cambiar contraseña", screen: "A4" },
            { icon: "school", label: "Mis cursos", meta: "3", screen: "C1" },
            { icon: "cost", label: "Mi consumo de IA", screen: "H2" },
          ] as { icon: string; label: string; meta?: string; screen: string }[]).map((it, i) => (
            <div key={i} onClick={() => nav(it.screen)} style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 14px", borderBottom: i < 2 ? `1px solid ${t.border}` : "none", minHeight: 48, cursor: "pointer" }}>
              <Ic id={it.icon} s={18} style={{ color: t.textSecondary }} />
              <span style={{ flex: 1, fontSize: 14 }}>{it.label}</span>
              {it.meta && <span style={{ fontSize: 12, color: t.textSecondary }}>{it.meta}</span>}
              <Ic id="right" s={15} style={{ color: t.textSecondary }} />
            </div>
          ))}
        </Card>
        <div style={{ height: 8 }} />
        <Btn t={t} variant="dan" onClick={() => nav("A2")}><Ic id="logout" s={18} />Cerrar sesión</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenH1({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>} title="Sincronización" />
      <Banner t={t}><Ic id="sync" s={16} /><span style={{ fontSize: 12 }}>Sincronizando 2 apuntes…</span></Banner>
      <Body flush>
        {[
          { title: "Integrales impropias · p.1", date: "12 ago · 9:41 AM", state: "sync", progress: 72 },
          { title: "Criterio de comparación · p.1", date: "7 ago · 8:20 AM", state: "queue" },
          { title: "Series de potencias · p.2", date: "2 ago · 7:55 AM", state: "done" },
          { title: "Prueba de la integral · p.1", date: "28 jul · 10:12 AM", state: "done" },
        ].map((it, i) => (
          <div key={i} onClick={() => nav("E2")} style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 14px", borderBottom: `1px solid ${t.border}`, minHeight: 64, cursor: "pointer" }}>
            <Thumb t={t}><Ic id="notes" s={18} /></Thumb>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13 }}>{it.title}</div>
              <div style={{ fontSize: 11, color: t.textSecondary }}>{it.date}</div>
              {it.state === "sync" && (
                <div style={{ height: 3, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", marginTop: 6 }}>
                  <div style={{ width: `${it.progress}%`, height: "100%", background: t.accent }} />
                </div>
              )}
            </div>
            {it.state === "done" && <Ic id="check" s={16} style={{ color: t.verified }} />}
            {it.state === "queue" && <Badge bg={t.accentSoft} color={t.accentText}>EN COLA</Badge>}
          </div>
        ))}
      </Body>
      <GestureBar t={t} />
    </>
  )
}

function ScreenG1({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1a")}><Ic id="menu" s={22} /></Tap>} title="Cálculo II" sub="Estudio de" right={<Tap t={t}><Ic id="target" s={22} /></Tap>} />
      <Body>
        <div onClick={() => nav("G2")} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 12, padding: 16, marginBottom: 12, cursor: "pointer" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Ic id="study" s={22} style={{ color: t.accentText }} />
            <span style={{ fontSize: 18, fontWeight: 500, flex: 1 }}>12 flashcards</span>
            <Ic id="right" s={18} style={{ color: t.textSecondary }} />
          </div>
          <div style={{ fontSize: 14, lineHeight: 1.5, color: t.textSecondary, marginTop: 8 }}>Generadas de los temas parciales. 4 son de la corrección de ayer.</div>
        </div>
        <div onClick={() => nav("G3")} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 12, padding: 16, marginBottom: 12, cursor: "pointer" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Ic id="coverage" s={22} style={{ color: t.accentText }} />
            <span style={{ fontSize: 18, fontWeight: 500, flex: 1 }}>Quiz de 5 preguntas</span>
            <Ic id="right" s={18} style={{ color: t.textSecondary }} />
          </div>
          <div style={{ fontSize: 14, lineHeight: 1.5, color: t.textSecondary, marginTop: 8 }}>Sobre los cinco vacíos con menos evidencia</div>
        </div>
        <div style={{ fontSize: 13, lineHeight: 1.5, color: t.textSecondary, padding: "8px 2px" }}>El material de estudio sale de la cobertura, no de un temario genérico: solo pregunta lo que tus apuntes no sostienen.</div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── C3 Centro de curso ────────────────────────────────────────────────────────
function ScreenC3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("C1")}><Ic id="back" /></Tap>}>
        <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.2, paddingLeft: 4 }}>
          <span style={{ fontSize: 11, color: t.textSecondary, letterSpacing: ".3px" }}>Centro de</span>
          <span style={{ fontSize: 18, fontWeight: 500 }}>Cálculo II</span>
        </div>
        <span style={{ flex: 1 }} />
        <Tap t={t} onClick={() => nav("D1")}><Ic id="camera" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div>
              <div style={{ fontSize: 18, fontWeight: 500 }}>Cálculo II</div>
              <div style={{ fontSize: 13, color: t.textSecondary, marginTop: 2, fontFamily: "ui-monospace,monospace" }}>EIF411</div>
            </div>
            <span style={{ fontSize: 26, fontWeight: 600, color: t.accentText }}>68%</span>
          </div>
          <CovBar t={t} v={54} r={20} e={14} u={12} style={{ margin: "12px 0 6px" }} />
          <div style={{ fontSize: 12, color: t.textSecondary }}>Prof. Maikol Guzmán · 27 estudiantes</div>
        </Card>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
          {([
            { icon: "notes", label: "Mis apuntes", sub: "14 apuntes", screen: "E1" },
            { icon: "coverage", label: "Cobertura", sub: "33 temas", screen: "F1" },
            { icon: "study", label: "Estudio", sub: "13 pendientes", screen: "G1" },
            { icon: "gaps", label: "Qué cambió", sub: "+6 pts esta semana", screen: "F3" },
          ] as { icon: string; label: string; sub: string; screen: string }[]).map(item => (
            <div key={item.icon} onClick={() => nav(item.screen)} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 12, padding: 14, cursor: "pointer" }}>
              <Ic id={item.icon} style={{ color: t.accentText, marginBottom: 8 }} />
              <div style={{ fontSize: 15, fontWeight: 500 }}>{item.label}</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>{item.sub}</div>
            </div>
          ))}
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Acceso rápido</div>
        <Card t={t} tight>
          <Item t={t} compact onClick={() => nav("E2")}><Ic id="notes" /><span style={{ flex: 1 }}>Mapa de confianza</span><Ic id="right" style={{ color: t.textSecondary }} /></Item>
          <Item t={t} compact onClick={() => nav("D1")}><Ic id="camera" /><span style={{ flex: 1 }}>Nueva captura</span><Ic id="right" style={{ color: t.textSecondary }} /></Item>
          <Item t={t} compact onClick={() => nav("H1")}><Ic id="sync" /><span style={{ flex: 1 }}>Cola de sincronización</span><Ic id="right" style={{ color: t.textSecondary }} /></Item>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── E3 Corregir fragmento ────────────────────────────────────────────────────
function ScreenE3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("E2")}><Ic id="close" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Corregir fragmento</span>
        <span style={{ flex: 1 }} />
        <span style={{ color: t.accentText, fontSize: 16, fontWeight: 500, paddingRight: 10, cursor: "pointer" }}>Guardar</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
          <Badge t={t} bg={t.uncertainSoft} color={t.uncertain}>INCIERTO</Badge>
          <span style={{ fontSize: 12, color: t.textSecondary }}>confianza 0.41 · umbral 0.70</span>
        </div>
        <div style={{ background: `repeating-linear-gradient(45deg,${t.surfaceHigh},${t.surfaceHigh} 6px,${t.border} 6px,${t.border} 12px)`, border: `1px solid ${t.border}`, borderRadius: 8, height: 110, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary, fontSize: 12, marginBottom: 6 }}>Recorte original de la región</div>
        <div style={{ textAlign: "center", fontSize: 12, color: t.textSecondary, marginBottom: 18 }}>Región 12 de 18</div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Lo que leyó el motor</div>
        <div style={{ background: t.surfaceHigh, border: `2px solid ${t.accentLine}`, borderRadius: 8, padding: "10px 14px", minHeight: 80, marginBottom: 12, lineHeight: 1.6 }}>
          <div style={{ fontSize: 11, color: t.accentText, marginBottom: 2 }}>TEXTO RECONOCIDO</div>
          <div>converge; en caso contrario <span style={{ background: t.uncertainSoft, borderRadius: 3, padding: "0 2px" }}>dvierge</span></div>
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Sugerencias del glosario</div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" as const, marginBottom: 18 }}>
          {["diverge", "divergente", "deriva"].map((s, i) => (
            <span key={s} style={{ padding: "6px 14px", borderRadius: 999, fontSize: 14, background: i === 0 ? t.accentSoft : t.neutralSoft, color: i === 0 ? t.accentText : t.textSecondary, cursor: "pointer" }}>{s}</span>
          ))}
        </div>
        <Card t={t} style={{ background: t.accentFaint, borderColor: t.accentLine }}>
          <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
            <div style={{ width: 20, height: 20, borderRadius: 4, border: `2px solid ${t.accent}`, background: t.accent, color: t.onAccent, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}><Ic id="check" style={{ width: 14, height: 14 }} /></div>
            <div>
              <div style={{ fontSize: 15 }}>Aportar al glosario del curso</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4, lineHeight: 1.5 }}>Tu corrección sube la confianza de esta palabra para todo el grupo.</div>
            </div>
          </div>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── K1 Ajustes ────────────────────────────────────────────────────────────────
function ScreenK1({ t, nav }: { t: Tok; nav: Nav }) {
  const [tog, setTog] = useState({ savePhoto: true, rejectLow: true, allowN2: true, confirmN3: true, wifiOnly: false, notifyDone: true })
  const sw = (key: keyof typeof tog) => {
    const on = tog[key]
    return (
      <div onClick={() => setTog(s => ({ ...s, [key]: !s[key] }))} style={{ width: 44, height: 24, borderRadius: 999, background: on ? t.accent : t.border, position: "relative", flexShrink: 0, cursor: "pointer" }}>
        <div style={{ position: "absolute", top: 3, left: on ? 23 : 3, width: 18, height: 18, borderRadius: 999, background: on ? t.onAccent : t.textSecondary, transition: "left .15s" }} />
      </div>
    )
  }
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav(roleHome === "B4" ? "B1c" : roleHome === "B3" ? "B1b" : "B1a")}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 22, fontWeight: 500, letterSpacing: "-.2px", paddingLeft: 4 }}>Ajustes</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Captura</div>
        <Card t={t} tight>
          <Item t={t} compact><span style={{ flex: 1 }}>Guardar la foto original</span>{sw("savePhoto")}</Item>
          <Item t={t} compact><span style={{ flex: 1 }}>Rechazar fotos de baja calidad</span>{sw("rejectLow")}</Item>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Procesamiento</div>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: 16 }}>Umbral de confianza</span>
            <span style={{ fontFamily: "ui-monospace,monospace", color: t.accentText }}>0.70</span>
          </div>
          <div style={{ height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", margin: "12px 0 8px" }}>
            <div style={{ width: "70%", height: "100%", background: t.accent }} />
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>Por debajo de este valor la región escala al siguiente motor. Más alto significa más precisión y más llamadas.</div>
        </Card>
        <Card t={t} tight>
          <Item t={t} compact><span style={{ flex: 1 }}>Permitir escalar a visión (N2)</span>{sw("allowN2")}</Item>
          <Item t={t} compact><span style={{ flex: 1 }}>Pedir confirmación antes de N3</span>{sw("confirmN3")}</Item>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Sincronización</div>
        <Card t={t} tight>
          <Item t={t} compact><span style={{ flex: 1 }}>Sincronizar solo con wifi</span>{sw("wifiOnly")}</Item>
          <Item t={t} compact><span style={{ flex: 1 }}>Avisar cuando termine la cola</span>{sw("notifyDone")}</Item>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Acerca de</div>
        <Card t={t} tight>
          <Item t={t} compact onClick={() => nav("H1")}><span style={{ flex: 1 }}>Cola de sincronización</span><span style={{ fontSize: 13, color: t.textSecondary, marginRight: 4 }}>2 pendientes</span><Ic id="right" style={{ color: t.textSecondary, width: 16, height: 16 }} /></Item>
          <Item t={t} compact><span style={{ flex: 1 }}>Versión</span><span style={{ fontFamily: "ui-monospace,monospace", fontSize: 13, color: t.textSecondary }}>1.0.0 · X-Ray</span></Item>
        </Card>
        <div style={{ height: 8 }} />
        <Btn t={t} variant="dan" onClick={() => nav("A2")}><Ic id="logout" s={18} />Cerrar sesión</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── F2 Detalle de tema ────────────────────────────────────────────────────────
function ScreenF2({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("F1")}><Ic id="back" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>3.3 Comparación</span>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
            <svg width="11" height="11"><rect width="11" height="11" rx="2" fill={t.repaired} /></svg>
            <Badge t={t} bg={t.repairedSoft} color={t.repaired}>PARCIAL</Badge>
            <span style={{ flex: 1 }} />
            <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 14, color: t.textSecondary }}>0.54</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 500, lineHeight: 1.4 }}>Criterio de comparación para series de términos positivos</div>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "0 0 8px" }}>Por qué está en este estado</div>
        <Card t={t}>
          {[
            { ok: true, label: "Enunciado del criterio", meta: "2 apuntes" },
            { ok: true, label: "Ejemplo resuelto", meta: "1 apunte" },
            { ok: false, label: "Criterio de comparación en el límite", meta: "sin evidencia" },
            { ok: false, label: "Condición de convergencia", meta: "sin evidencia" },
          ].map(row => (
            <div key={row.label} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 0", borderBottom: `1px solid ${t.border}`, fontSize: 14 }}>
              <Ic id={row.ok ? "check" : "close"} style={{ color: row.ok ? t.verified : t.escalated, width: 18, height: 18 }} />
              <span style={{ flex: 1 }}>{row.label}</span>
              <span style={{ fontSize: 12, color: t.textSecondary }}>{row.meta}</span>
            </div>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Dónde aparece en tus apuntes</div>
        <Card t={t} tight>
          <Item t={t} compact onClick={() => nav("E2")}>
            <Thumb t={t} style={{ width: 36, height: 36 }}><Ic id="notes" style={{ width: 18, height: 18 }} /></Thumb>
            <div style={{ flex: 1 }}><div>Criterio de comparación</div><div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>7 ago · página 2</div></div>
            <Ic id="right" style={{ color: t.textSecondary, width: 18, height: 18 }} />
          </Item>
          <Item t={t} compact onClick={() => nav("E2")}>
            <Thumb t={t} style={{ width: 36, height: 36 }}><Ic id="notes" style={{ width: 18, height: 18 }} /></Thumb>
            <div style={{ flex: 1 }}><div>Series de potencias</div><div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>5 ago · página 1</div></div>
            <Ic id="right" style={{ color: t.textSecondary, width: 18, height: 18 }} />
          </Item>
        </Card>
        <Btn t={t} onClick={() => nav("G1")}><Ic id="study" style={{ width: 20, height: 20 }} />Generar refuerzo de este tema</Btn>
        <Btn t={t} variant="txt" onClick={() => nav("F1")}>Marcar como ya dominado</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── F3 Delta entre sesiones ───────────────────────────────────────────────────
function ScreenF3({ t, nav }: { t: Tok; nav: Nav }) {
  const TrustIcon = ({ type }: { type: "v"|"r"|"e"|"u" }) => {
    if (type === "v") return <svg width="12" height="12"><circle cx="6" cy="6" r="6" fill={t.verified} /></svg>
    if (type === "r") return <svg width="11" height="11"><rect width="11" height="11" rx="2" fill={t.repaired} /></svg>
    if (type === "e") return <svg width="14" height="12"><polygon points="7,0 14,12 0,12" fill={t.escalated} /></svg>
    return <svg width="12" height="12"><circle cx="6" cy="6" r="5.2" fill="none" stroke={t.uncertain} strokeWidth="1.5" strokeDasharray="2 2" /></svg>
  }
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("F1")}><Ic id="back" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Qué cambió</span>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t}>
          <div style={{ fontSize: 12, color: t.textSecondary }}>Del 8 al 15 de agosto</div>
          <div style={{ display: "flex", gap: 14, alignItems: "baseline", margin: "14px 0 4px" }}>
            <span style={{ fontSize: 26, fontWeight: 600, color: t.textSecondary }}>62%</span>
            <Ic id="right" style={{ color: t.textSecondary, width: 20, height: 20 }} />
            <span style={{ fontSize: 32, fontWeight: 600, color: t.accentText }}>68%</span>
            <Badge t={t} bg={t.verifiedSoft} color={t.verified} style={{ marginLeft: "auto" }}>+6 PUNTOS</Badge>
          </div>
          <div style={{ display: "flex", height: 6, borderRadius: 999, overflow: "hidden", margin: "0 0 4px", opacity: .45 }}>
            <div style={{ width: "48%", background: t.verified }} /><div style={{ width: "18%", background: t.repaired }} />
            <div style={{ width: "20%", background: t.escalated }} /><div style={{ width: "14%", background: t.uncertain }} />
          </div>
          <div style={{ display: "flex", height: 6, borderRadius: 999, overflow: "hidden" }}>
            <div style={{ width: "54%", background: t.verified }} /><div style={{ width: "20%", background: t.repaired }} />
            <div style={{ width: "14%", background: t.escalated }} /><div style={{ width: "12%", background: t.uncertain }} />
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 8 }}>4 apuntes nuevos · 11 páginas procesadas</div>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Temas que mejoraron</div>
        <Card t={t} tight>
          {[
            { label: "3.2 Series geométricas", from: "e", to: "v" },
            { label: "3.3 Comparación", from: "e", to: "r" },
            { label: "2.4 Integrales impropias", from: "r", to: "v" },
          ].map(row => (
            <Item t={t} compact key={row.label}>
              <span style={{ flex: 1, fontSize: 15 }}>{row.label}</span>
              <div style={{ display: "flex", alignItems: "center", gap: 7 }}>
                <TrustIcon type={row.from as "e"|"r"} />
                <Ic id="right" style={{ color: t.textSecondary, width: 18, height: 18 }} />
                <TrustIcon type={row.to as "v"|"r"} />
              </div>
            </Item>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Sigue sin evidencia</div>
        <Card t={t} tight>
          <Item t={t} compact>
            <TrustIcon type="e" />
            <div style={{ flex: 1 }}><div style={{ fontSize: 15 }}>3.4 Series de potencias</div><div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>Sin cambios desde el 29 de julio</div></div>
          </Item>
          <Item t={t} compact>
            <TrustIcon type="u" />
            <div style={{ flex: 1 }}><div style={{ fontSize: 15 }}>3.5 Radio de convergencia</div><div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>Hay apunte, pero ilegible</div></div>
            <Badge t={t} bg={t.uncertainSoft} color={t.uncertain}>REVISAR</Badge>
          </Item>
        </Card>
        <Btn t={t} variant="sec" onClick={() => nav("E2")}>Ver el apunte ilegible</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── I3 Publicar temario ───────────────────────────────────────────────────────
function ScreenI3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("I1")}><Ic id="back" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Temario · EIF411</span>
        <span style={{ flex: 1 }} />
        <span onClick={() => nav("B3")} style={{ color: t.accentText, fontSize: 16, fontWeight: 500, paddingRight: 10, cursor: "pointer" }}>Publicar</span>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t} style={{ borderColor: t.accentLine }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <Ic id="pdf" style={{ color: t.accentText, width: 32, height: 32 }} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16, fontWeight: 500 }}>programa-EIF411.pdf</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>1.2 MB · procesado hace 2 minutos</div>
            </div>
            <Ic id="refresh" style={{ color: t.textSecondary, width: 20, height: 20 }} />
          </div>
        </Card>
        <div style={{ display: "flex", gap: 10, alignItems: "flex-start", margin: "2px 0 14px" }}>
          <Ic id="check" style={{ color: t.verified, marginTop: 1, width: 18, height: 18 }} />
          <span style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>Se extrajeron <b style={{ color: t.textPrimary }}>6 unidades y 33 temas</b>. Revisalos antes de publicar: el temario se procesa una sola vez.</span>
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Estructura detectada</div>
        <Card t={t} tight>
          <Item t={t} compact style={{ background: t.surfaceHigh }}>
            <Ic id="down" style={{ color: t.accentText, width: 18, height: 18 }} />
            <span style={{ flex: 1, fontWeight: 500 }}>3 · Series</span>
            <span style={{ fontSize: 12, color: t.textSecondary }}>5 temas</span>
            <Ic id="edit" style={{ color: t.textSecondary, width: 18, height: 18 }} />
          </Item>
          <Item t={t} compact style={{ paddingLeft: 38 }}>
            <span style={{ flex: 1, fontSize: 15 }}>3.1 Sucesiones</span>
            <Ic id="edit" style={{ color: t.textSecondary, width: 18, height: 18 }} />
          </Item>
          <Item t={t} compact style={{ paddingLeft: 38 }}>
            <span style={{ flex: 1, fontSize: 15 }}>3.2 Series geométricas</span>
            <Ic id="edit" style={{ color: t.textSecondary, width: 18, height: 18 }} />
          </Item>
          <Item t={t} compact style={{ paddingLeft: 38, background: t.alertFaint }}>
            <span style={{ flex: 1, fontSize: 15, color: t.alert }}>3.3 Criterío de comparacion</span>
            <Ic id="edit" style={{ color: t.alert, width: 18, height: 18 }} />
          </Item>
          <Item t={t} compact>
            <div style={{ display: "flex", alignItems: "center", gap: 14, flex: 1 }}>
              <Ic id="right" style={{ color: t.textSecondary, width: 18, height: 18 }} />
              <span style={{ fontWeight: 500 }}>4 · Cálculo vectorial</span>
            </div>
            <span style={{ fontSize: 12, color: t.textSecondary }}>7 temas</span>
          </Item>
        </Card>
        <Btn t={t} variant="sec"><Ic id="add" style={{ width: 20, height: 20 }} />Agregar un tema a mano</Btn>
        <div style={{ display: "flex", gap: 10, alignItems: "flex-start", marginTop: 14 }}>
          <Ic id="warn" style={{ color: t.textSecondary, marginTop: 1, width: 18, height: 18 }} />
          <span style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>Si la extracción falla, se puede escribir el temario completo a mano. El curso nunca queda bloqueado por el PDF.</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── I4 Glosario docente ───────────────────────────────────────────────────────
function ScreenI4({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("I1")}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Glosario</span>
        <span style={{ flex: 1 }} />
        <Tap t={t}><Ic id="search" /></Tap>
        <Tap t={t}><Ic id="add" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t} style={{ background: t.accentFaint, borderColor: t.accentLine }}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
            <Ic id="target" style={{ color: t.accentText, width: 20, height: 20 }} />
            <div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Esto no es un diccionario</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4, lineHeight: 1.5 }}>Cada entrada sube la confianza del reconocimiento sobre ese símbolo para todo el curso.</div>
            </div>
          </div>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>42 entradas</div>
        <Card t={t} tight>
          {[
            { term: "Sumatoria", detail: "\\sum_{n=1}^{\\infty}", tag: "SÍMBOLO", tagC: t.repairedSoft, tagT: t.repaired, mono: true },
            { term: "Converge", detail: "conv. · cnv · converg.", tag: "TÉRMINO", tagC: t.accentSoft, tagT: t.accentText, mono: false },
            { term: "Radio de convergencia", detail: "R", tag: "TÉRMINO", tagC: t.accentSoft, tagT: t.accentText, mono: true },
            { term: "Integral impropia", detail: "\\int_a^{\\infty}", tag: "SÍMBOLO", tagC: t.repairedSoft, tagT: t.repaired, mono: true },
            { term: "Límite n→∞", detail: "\\lim_{n \\to \\infty}", tag: "SÍMBOLO", tagC: t.repairedSoft, tagT: t.repaired, mono: true },
          ].map(e => (
            <Item t={t} key={e.term}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 16 }}>{e.term}</div>
                <div style={{ fontSize: 13, color: t.textSecondary, marginTop: 2, fontFamily: e.mono ? "ui-monospace,monospace" : undefined }}>{e.detail}</div>
              </div>
              <Badge t={t} bg={e.tagC} color={e.tagT}>{e.tag}</Badge>
            </Item>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Sugeridas por correcciones</div>
        <Card t={t} tight>
          <Item t={t} compact>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15 }}>diverge</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>corregido 7 veces esta semana</div>
            </div>
            <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
              <Ic id="close" style={{ color: t.textSecondary, width: 20, height: 20, cursor: "pointer" }} />
              <Ic id="check" style={{ color: t.verified, width: 20, height: 20, cursor: "pointer" }} />
            </div>
          </Item>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── E4 Visor de fórmula ───────────────────────────────────────────────────────
function ScreenE4({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("E2")}><Ic id="close" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Fórmula · región 7</span>
        <span style={{ flex: 1 }} />
        <Badge t={t} bg={t.repairedSoft} color={t.repaired} style={{ marginRight: 12 }}>N1.5</Badge>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Recorte original</div>
        <div style={{ background: `repeating-linear-gradient(45deg,${t.surfaceHigh},${t.surfaceHigh} 6px,${t.border} 6px,${t.border} 12px)`, border: `1px solid ${t.border}`, borderRadius: 8, height: 120, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary, fontSize: 12, marginBottom: 16 }}>Fotografía de la región</div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Transcripción</div>
        <div style={{ background: t.surfaceHigh, border: `1px solid ${t.border}`, borderRadius: 8, padding: "22px 14px", textAlign: "center", fontSize: 22, marginBottom: 12 }}>
          ∫<sub style={{ fontSize: 12 }}>a</sub><sup style={{ fontSize: 12 }}>∞</sup> f(x) dx = lim<sub style={{ fontSize: 12 }}>b→∞</sub> ∫<sub style={{ fontSize: 12 }}>a</sub><sup style={{ fontSize: 12 }}>b</sup> f(x) dx
        </div>
        <Card t={t} style={{ padding: "12px 14px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
            <span style={{ fontSize: 12, color: t.textSecondary }}>LaTeX generado</span>
            <Ic id="copy" style={{ color: t.accentText, width: 18, height: 18 }} />
          </div>
          <div style={{ fontFamily: "ui-monospace,monospace", fontSize: 13, lineHeight: 1.6, color: t.textSecondary }}>
            {"\\int_a^{\\infty} f(x)\\,dx = \\lim_{b \\to \\infty} \\int_a^b f(x)\\,dx"}
          </div>
        </Card>
        <Card t={t} tight>
          <Item t={t} compact><Ic id="check" style={{ color: t.verified, width: 18, height: 18 }} /><span style={{ flex: 1, fontSize: 15 }}>Compila con JLaTeXMath</span></Item>
          <Item t={t} compact><Ic id="target" style={{ color: t.textSecondary, width: 18, height: 18 }} /><span style={{ flex: 1, fontSize: 15 }}>Confianza del motor</span><span style={{ fontFamily: "ui-monospace,monospace", fontSize: 14, color: t.textSecondary }}>0.86</span></Item>
          <Item t={t} compact><Ic id="cost" style={{ color: t.textSecondary, width: 18, height: 18 }} /><span style={{ flex: 1, fontSize: 15 }}>Resuelto por</span><span style={{ fontSize: 14, color: t.textSecondary }}>SimpleTex · gratuito</span></Item>
        </Card>
        <div style={{ display: "flex", gap: 10 }}>
          <Btn t={t} variant="sec" onClick={() => nav("E3")} style={{ flex: 1, height: 40, fontSize: 14 }}>Corregir</Btn>
          <Btn t={t} onClick={() => nav("E2")} style={{ flex: 1, height: 40, fontSize: 14 }}><Ic id="check" style={{ width: 18, height: 18 }} />Está bien</Btn>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── E5 Procedencia de región ──────────────────────────────────────────────────
function ScreenE5({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("E2")}><Ic id="back" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Región 12</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ background: `repeating-linear-gradient(45deg,${t.surfaceHigh},${t.surfaceHigh} 6px,${t.border} 6px,${t.border} 12px)`, border: `1px solid ${t.border}`, borderRadius: 8, height: 96, display: "flex", alignItems: "center", justifyContent: "center", color: t.textSecondary, fontSize: 12, marginBottom: 14 }}>Recorte de la región</div>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Badge t={t} bg={t.uncertainSoft} color={t.uncertain}>INCIERTO</Badge>
            <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 20, color: t.uncertain }}>0.41</span>
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 8 }}>Umbral configurado: 0.70</div>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Cómo se calculó</div>
        <Card t={t}>
          {[
            { label: "Confianza del OCR", pct: 38, color: t.uncertain, val: "0.38" },
            { label: "Coincide con el glosario", pct: 0, color: t.accent, val: "0.00" },
            { label: "Palabra del diccionario", pct: 0, color: t.accent, val: "0.00" },
            { label: "Coherencia con región vecina", pct: 72, color: t.verified, val: "0.72" },
          ].map(row => (
            <div key={row.label} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 0", borderBottom: `1px solid ${t.border}`, fontSize: 14 }}>
              <span style={{ flex: 1 }}>{row.label}</span>
              <div style={{ width: 70, height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", flexShrink: 0 }}>
                <div style={{ width: `${row.pct}%`, height: "100%", background: row.color }} />
              </div>
              <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary, width: 36, textAlign: "right" }}>{row.val}</span>
            </div>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Recorrido en la escalera</div>
        <Card t={t} tight>
          {[
            { icon: "check", color: t.verified, label: "N0 · preprocesada", meta: "0.02 s" },
            { icon: "check", color: t.verified, label: "N1 · ML Kit", meta: "0.06 s" },
            { icon: "close", color: t.textSecondary, label: "N1.5 · no es fórmula", meta: "omitido" },
            { icon: "check", color: t.escalated, label: "N2 · visión, en lote", meta: "1.4 s" },
          ].map(r => (
            <Item t={t} compact key={r.label}>
              <Ic id={r.icon} style={{ color: r.color, width: 18, height: 18 }} />
              <span style={{ flex: 1, fontSize: 15, color: r.color === t.textSecondary ? t.textSecondary : undefined }}>{r.label}</span>
              <span style={{ fontSize: 12, color: t.textSecondary }}>{r.meta}</span>
            </Item>
          ))}
        </Card>
        <Card t={t} onClick={() => nav("E2")} style={{ background: t.escalatedFaint, borderColor: t.escalatedLine, cursor: "pointer" }}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
            <Ic id="warn" style={{ color: t.escalated, width: 20, height: 20 }} />
            <div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Enviar la página completa (N3)</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4, lineHeight: 1.5 }}>Una llamada adicional al modelo de pago. Último recurso.</div>
            </div>
          </div>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── G2 Flashcards ─────────────────────────────────────────────────────────────
function ScreenG2({ t, nav }: { t: Tok; nav: Nav }) {
  const [flipped, setFlipped] = useState(false)
  const [cardIdx, setCardIdx] = useState(0)
  const cards = [
    { q: "¿Qué condición deben cumplir las series para aplicar el criterio de comparación directa?", a: "Ambas series deben ser de términos positivos y debe cumplirse aₙ ≤ bₙ para todo n a partir de un índice.", src: "De tu apunte del 7 ago" },
    { q: "¿Cuándo converge una serie geométrica ∑ arⁿ?", a: "Cuando |r| < 1. La suma es a/(1−r).", src: "De tu apunte del 3 ago" },
    { q: "Enunciá el criterio de la integral para series de términos positivos.", a: "La serie ∑f(n) converge si y solo si la integral ∫f(x)dx de 1 a ∞ converge.", src: "De tu apunte del 5 ago" },
  ]
  const card = cards[cardIdx % cards.length]
  const total = 12
  const current = cardIdx + 1
  const advance = () => { setFlipped(false); setCardIdx(i => i + 1) }
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("G1")}><Ic id="close" /></Tap>}>
        <div style={{ flex: 1, padding: "0 8px" }}>
          <div style={{ height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden" }}>
            <div style={{ width: `${(current / total) * 100}%`, height: "100%", background: t.accent, transition: "width .3s" }} />
          </div>
        </div>
        <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary, paddingRight: 12 }}>{current} / {total}</span>
      </AppBar>
      <Body t={t} style={{ display: "flex", flexDirection: "column" }}>
        <div style={{ fontSize: 12, color: t.textSecondary, marginBottom: 12 }}>Unidad 3 · {flipped ? "Respuesta" : "Pregunta"} — tocá la tarjeta para voltear</div>
        <div onClick={() => setFlipped(f => !f)} style={{ flex: 1, background: t.surface, border: `1px solid ${flipped ? t.accentLine : t.border}`, borderRadius: 12, padding: 24, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 16, cursor: "pointer", textAlign: "center", minHeight: 240, transition: "border-color .2s" }}>
          <Badge t={t} bg={flipped ? t.accentSoft : t.neutralSoft} color={flipped ? t.accentText : t.textSecondary}>{flipped ? "RESPUESTA" : "PREGUNTA"}</Badge>
          <div style={{ fontSize: flipped ? 16 : 19, lineHeight: 1.55, color: flipped ? t.textSecondary : t.textPrimary }}>
            {flipped ? card.a : card.q}
          </div>
          {flipped && <Badge t={t} bg={t.neutralSoft} color={t.textSecondary}>{card.src.toUpperCase()}</Badge>}
        </div>
        {flipped ? (
          <>
            <div style={{ fontSize: 12, color: t.textSecondary, textAlign: "center", margin: "14px 0 10px" }}>¿Qué tan bien la recordaste?</div>
            <div style={{ display: "flex", gap: 8 }}>
              {[
                { label: "Otra vez", color: t.alert },
                { label: "Difícil", color: t.escalated },
                { label: "Bien", color: t.repaired },
                { label: "Fácil", color: t.verified },
              ].map(b => (
                <div key={b.label} onClick={advance} style={{ flex: 1, height: 44, border: `1px solid ${b.color}`, color: b.color, borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 13, fontWeight: 500, cursor: "pointer" }}>{b.label}</div>
              ))}
            </div>
          </>
        ) : (
          <div style={{ height: 16 }} />
        )}
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── G3 Quiz ───────────────────────────────────────────────────────────────────
function ScreenG3({ t, nav }: { t: Tok; nav: Nav }) {
  const [sel, setSel] = useState<number | null>(null)
  const [answered, setAnswered] = useState(false)
  const correct = 1
  const opts = ["R = 1", "R = ∞", "R = 0", "R = e"]
  const pick = (i: number) => { if (!answered) setSel(i) }
  const submit = () => { if (sel !== null) setAnswered(true) }
  const borderFor = (i: number) => {
    if (!answered) return sel === i ? t.accentText : t.border
    if (i === correct) return t.verified
    if (i === sel && sel !== correct) return t.alert
    return t.border
  }
  const bgFor = (i: number) => {
    if (!answered) return sel === i ? t.accentFaint : t.surface
    if (i === correct) return t.verifiedSoft
    if (i === sel && sel !== correct) return t.alertFaint
    return t.surface
  }
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("G1")}><Ic id="close" /></Tap>}>
        <div style={{ flex: 1, padding: "0 8px" }}>
          <div style={{ height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden" }}>
            <div style={{ width: "40%", height: "100%", background: t.accent }} />
          </div>
        </div>
        <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary, paddingRight: 12 }}>2 / 5</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 14 }}>
          <Badge t={t} bg={t.escalatedSoft} color={t.escalated}>TEMA AUSENTE</Badge>
          <span style={{ fontSize: 12, color: t.textSecondary }}>3.4 Series de potencias</span>
        </div>
        <div style={{ fontSize: 19, lineHeight: 1.55, marginBottom: 22 }}>
          ¿Cuál es el radio de convergencia de la serie <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 17 }}>∑ xⁿ / n!</span>?
        </div>
        {opts.map((label, i) => (
          <div key={label} onClick={() => pick(i)} style={{ background: bgFor(i), border: `1.5px solid ${borderFor(i)}`, borderRadius: 12, padding: "14px", marginBottom: 10, display: "flex", alignItems: "center", gap: 12, cursor: answered ? "default" : "pointer", transition: "border-color .15s, background .15s" }}>
            <div style={{ width: 22, height: 22, borderRadius: 999, border: `2px solid ${borderFor(i)}`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              {answered && i === correct && <Ic id="check" style={{ width: 14, height: 14, color: t.verified }} />}
              {answered && i === sel && sel !== correct && <Ic id="close" style={{ width: 14, height: 14, color: t.alert }} />}
              {!answered && sel === i && <div style={{ width: 10, height: 10, borderRadius: 999, background: t.accentText }} />}
              <span style={{ fontSize: 11, fontWeight: 600, color: answered ? (i === correct ? t.verified : i === sel ? t.alert : t.textSecondary) : (sel === i ? t.accentText : t.textSecondary) }}>
                {!answered || (i !== correct && i !== sel) ? ["A","B","C","D"][i] : ""}
              </span>
            </div>
            <span style={{ fontSize: 16 }}>{label}</span>
          </div>
        ))}
        <div style={{ height: 8 }} />
        {!answered ? (
          <Btn t={t} onClick={submit}>Responder</Btn>
        ) : (
          <Btn t={t} onClick={() => nav("G4")}>Ver el resultado</Btn>
        )}
        {answered && (
          <div style={{ background: t.verifiedSoft, border: `1px solid ${t.verifiedLine}`, borderRadius: 10, padding: 14, marginTop: 12 }}>
            <div style={{ fontSize: 14, fontWeight: 500, color: t.verified, marginBottom: 4 }}>¡Correcto!</div>
            <div style={{ fontSize: 13, color: t.textSecondary, lineHeight: 1.5 }}>La serie exponencial ∑ xⁿ/n! converge para todo x ∈ ℝ, por lo que su radio de convergencia es infinito.</div>
          </div>
        )}
        <div style={{ display: "flex", gap: 10, marginTop: 14, alignItems: "flex-start" }}>
          <Ic id="shield" style={{ color: t.textSecondary, marginTop: 1, width: 18, height: 18 }} />
          <span style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>La corrección es local. Las 5 preguntas se generaron en una sola llamada.</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── G4 Resultado quiz ─────────────────────────────────────────────────────────
function ScreenG4({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("G1")}><Ic id="close" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Resultado</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ textAlign: "center", padding: "14px 0 20px" }}>
          <svg width="128" height="128" viewBox="0 0 128 128">
            <circle cx="64" cy="64" r="54" fill="none" stroke={t.surfaceHigh} strokeWidth="12" />
            <circle cx="64" cy="64" r="54" fill="none" stroke={t.accentText} strokeWidth="12"
              strokeLinecap="round" strokeDasharray="238 339" transform="rotate(-90 64 64)" />
            <text x="64" y="60" textAnchor="middle" fill={t.textPrimary} fontSize="34" fontWeight="600" fontFamily="Inter">7</text>
            <text x="64" y="82" textAnchor="middle" fill={t.textSecondary} fontSize="15" fontFamily="Inter">de 10</text>
          </svg>
          <div style={{ fontSize: 18, fontWeight: 500, marginTop: 10 }}>Series de potencias sigue floja</div>
          <div style={{ fontSize: 14, color: t.textSecondary, marginTop: 4 }}>Los 3 fallos son del mismo tema.</div>
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Revisión</div>
        <Card t={t} tight>
          {[
            { ok: true, label: "1 · Definición de sucesión" },
            { ok: true, label: "2 · Serie geométrica" },
            { ok: false, label: "3 · Radio de convergencia" },
            { ok: false, label: "4 · Serie exponencial" },
          ].map(r => (
            <Item t={t} compact key={r.label} style={{ background: r.ok ? undefined : t.alertFaint }}>
              <Ic id={r.ok ? "check" : "close"} style={{ color: r.ok ? t.verified : t.alert, width: 18, height: 18 }} />
              <span style={{ flex: 1, fontSize: 15 }}>{r.label}</span>
              <Ic id="right" style={{ color: t.textSecondary, width: 18, height: 18 }} />
            </Item>
          ))}
        </Card>
        <Card t={t} style={{ background: t.accentFaint, borderColor: t.accentLine }}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
            <Ic id="calendar" style={{ color: t.accentText, width: 20, height: 20 }} />
            <div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>3 ítems reprogramados para mañana</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4 }}>Los aciertos vuelven en 6 días.</div>
            </div>
          </div>
        </Card>
        <Btn t={t} onClick={() => nav("F1")}><Ic id="coverage" style={{ width: 20, height: 20 }} />Ver la cobertura actualizada</Btn>
        <Btn t={t} variant="sec" onClick={() => nav("I4")}><Ic id="glossary" style={{ width: 20, height: 20 }} />Explicame el radio de convergencia</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── H2 Consumo de IA ──────────────────────────────────────────────────────────
function ScreenH2({ t, nav }: { t: Tok; nav: Nav }) {
  const isAdmin = roleHome === "B4"
  const isTeacher = roleHome === "B3"
  const drawerScreen = roleHome === "B4" ? "B1c" : roleHome === "B3" ? "B1b" : "B1a"
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav(drawerScreen)}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Consumo{isAdmin ? " del sistema" : isTeacher ? " del curso" : " de IA"}</span>
        <span style={{ flex: 1 }} />
        <Tap t={t}><Ic id="calendar" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t} style={{ borderColor: t.verifiedLine }}>
          <div style={{ fontSize: 12, color: t.textSecondary }}>
            Agosto · {isAdmin ? "Sistema completo · 142 usuarios" : isTeacher ? "EIF411 · 27 estudiantes" : "Cálculo II"}
          </div>
          {isAdmin ? (
            <>
              <div style={{ display: "flex", gap: 10, alignItems: "baseline", marginTop: 6 }}>
                <span style={{ fontSize: 34, fontWeight: 600, color: t.accentText }}>64%</span>
                <span style={{ fontSize: 15, lineHeight: 1.35 }}>de las llamadas resueltas<br />sin servicio de pago</span>
              </div>
              <div style={{ height: 8, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", marginTop: 12 }}>
                <div style={{ width: "64%", height: "100%", background: t.accent }} />
              </div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 8 }}>1 842 de 2 900 llamadas de nivel 2 · cuota al 78%</div>
            </>
          ) : (
            <div style={{ display: "flex", gap: 10, alignItems: "baseline", marginTop: 6 }}>
              <span style={{ fontSize: 34, fontWeight: 600, color: t.verified }}>81%</span>
              <span style={{ fontSize: 15, lineHeight: 1.35 }}>de las regiones se resolvieron<br />sin llamadas de pago</span>
            </div>
          )}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Dónde se resolvió cada región</div>
        <Card t={t}>
          {[
            { icon: <svg width="12" height="12"><circle cx="6" cy="6" r="6" fill={t.verified} /></svg>, label: "N1 · ML Kit local", pct: 68, color: t.verified, count: 612 },
            { icon: <svg width="11" height="11"><rect width="11" height="11" rx="2" fill={t.repaired} /></svg>, label: "N1.5 · SimpleTex gratis", pct: 13, color: t.repaired, count: 117 },
            { icon: <svg width="14" height="12"><polygon points="7,0 14,12 0,12" fill={t.escalated} /></svg>, label: "N2 · visión, en lote", pct: 16, color: t.escalated, count: 144 },
            { icon: <svg width="12" height="12"><circle cx="6" cy="6" r="5.2" fill="none" stroke={t.uncertain} strokeWidth="1.5" strokeDasharray="2 2" /></svg>, label: "Sin resolver", pct: 3, color: t.uncertain, count: 27 },
          ].map(row => (
            <div key={row.label} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 0", borderBottom: `1px solid ${t.border}`, fontSize: 14 }}>
              {row.icon}
              <span style={{ flex: 1 }}>{row.label}</span>
              <div style={{ width: 64, height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden" }}>
                <div style={{ width: `${row.pct}%`, height: "100%", background: row.color }} />
              </div>
              <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary, width: 34, textAlign: "right" }}>{row.count}</span>
            </div>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Llamadas al modelo de pago</div>
        <Card t={t} tight>
          {[
            { label: "IA-00 · reparación por visión", count: 31 },
            { label: "IA-01 · reconstrucción", count: 47 },
            { label: "IA-02 · flashcards y quizzes", count: 9 },
            { label: "IA-05 · adjudicación semántica", count: 6 },
          ].map(r => (
            <Item t={t} compact key={r.label}>
              <span style={{ flex: 1, fontSize: 15 }}>{r.label}</span>
              <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 14, color: t.textSecondary }}>{r.count}</span>
            </Item>
          ))}
          <Item t={t} compact style={{ background: t.surfaceHigh }}>
            <span style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Total</span>
            <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 16, color: t.accentText }}>93</span>
          </Item>
        </Card>
        <div style={{ display: "flex", gap: 10, marginTop: 12, alignItems: "flex-start" }}>
          <Ic id="target" style={{ color: t.textSecondary, marginTop: 1, width: 18, height: 18 }} />
          <span style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>Sin la escalera, 47 páginas habrían requerido 47 llamadas de visión de página completa.</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── I5 Brechas del grupo ──────────────────────────────────────────────────────
function ScreenI5({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B3")}><Ic id="menu" /></Tap>}>
        <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.2, paddingLeft: 4 }}>
          <span style={{ fontSize: 11, color: t.textSecondary, letterSpacing: ".3px" }}>Brechas de</span>
          <span style={{ fontSize: 18, fontWeight: 500 }}>EIF411</span>
        </div>
        <span style={{ flex: 1 }} />
        <Tap t={t}><Ic id="upload" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        <Card t={t}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
            <div style={{ fontSize: 18, fontWeight: 500 }}>27 estudiantes</div>
            <span style={{ fontSize: 24, fontWeight: 600, color: t.accentText }}>54%</span>
          </div>
          <CovBar t={t} v={40} r={24} e={26} u={10} style={{ margin: "10px 0 6px" }} />
          <div style={{ fontSize: 12, color: t.textSecondary }}>Cobertura promedio del grupo · actualizada hoy</div>
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Temas con más vacíos</div>
        <Card t={t} tight>
          {[
            { icon: "e", label: "4.3 Sockets e hilos", pct: 70, count: "19/27" },
            { icon: "u", label: "6.1 Seguridad y permisos", pct: 52, count: "14/27" },
            { icon: "r", label: "3.2 Jetpack Compose", pct: 30, count: "8/27" },
          ].map(row => {
            const icon = row.icon === "e"
              ? <svg width="14" height="12"><polygon points="7,0 14,12 0,12" fill={t.escalated} /></svg>
              : row.icon === "u"
                ? <svg width="12" height="12"><circle cx="6" cy="6" r="5.2" fill="none" stroke={t.uncertain} strokeWidth="1.5" strokeDasharray="2 2" /></svg>
                : <svg width="11" height="11"><rect width="11" height="11" rx="2" fill={t.repaired} /></svg>
            const barColor = row.icon === "e" ? t.escalated : row.icon === "u" ? t.uncertain : t.repaired
            return (
              <Item t={t} key={row.label} onClick={() => nav("F2")}>
                {icon}
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 15 }}>{row.label}</div>
                  <div style={{ height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden", marginTop: 7 }}>
                    <div style={{ width: `${row.pct}%`, height: "100%", background: barColor }} />
                  </div>
                </div>
                <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary }}>{row.count}</span>
              </Item>
            )
          })}
        </Card>
        <Card t={t} style={{ background: t.uncertainFaint, borderColor: t.uncertainLine }}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
            <svg width="12" height="12" style={{ marginTop: 2, flexShrink: 0 }}><circle cx="6" cy="6" r="5.2" fill="none" stroke={t.uncertain} strokeWidth="1.5" strokeDasharray="2 2" /></svg>
            <div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>6.1 es un caso distinto</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4, lineHeight: 1.5 }}>11 de los 14 tienen apuntes del tema, pero ilegibles. No es que no lo estudiaran.</div>
            </div>
          </div>
        </Card>
        <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
          <Ic id="shield" style={{ color: t.textSecondary, marginTop: 1, width: 18, height: 18 }} />
          <span style={{ fontSize: 12, color: t.textSecondary, lineHeight: 1.5 }}>Datos agregados. El docente nunca ve apuntes individuales.</span>
        </div>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── B4 Inicio admin ───────────────────────────────────────────────────────────
function ScreenB4({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B1c")}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Administración</span>
        <span style={{ flex: 1 }} />
        <Tap t={t} onClick={() => nav("J1")}><Ic id="users" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
          <div onClick={() => nav("J1")} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 12, padding: 14, minHeight: 84, display: "flex", flexDirection: "column", justifyContent: "space-between", cursor: "pointer" }}>
            <span style={{ fontSize: 16, fontWeight: 500 }}>142</span>
            <span style={{ fontSize: 12, color: t.textSecondary }}>Usuarios activos</span>
          </div>
          <div onClick={() => nav("H2")} style={{ background: t.surface, border: `1px solid ${t.border}`, borderRadius: 12, padding: 14, minHeight: 84, display: "flex", flexDirection: "column", justifyContent: "space-between", cursor: "pointer" }}>
            <span style={{ fontSize: 16, fontWeight: 500 }}>64%</span>
            <span style={{ fontSize: 12, color: t.textSecondary }}>Sin llamadas de pago</span>
          </div>
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Estado del sistema</div>
        <Card t={t} tight>
          {[
            { icon: "check", color: t.verified, label: "API", meta: "142 ms" },
            { icon: "check", color: t.verified, label: "PostgreSQL", meta: "operativa" },
            { icon: "warn", color: t.escalated, label: "SimpleTex", meta: "cuota al 78%" },
            { icon: "check", color: t.verified, label: "Cola de sincronización", meta: "4 pendientes" },
          ].map(r => (
            <Item t={t} compact key={r.label}>
              <Ic id={r.icon} style={{ color: r.color, width: 18, height: 18 }} />
              <span style={{ flex: 1, fontSize: 15 }}>{r.label}</span>
              <span style={{ fontSize: 12, color: t.textSecondary }}>{r.meta}</span>
            </Item>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Reparto de roles</div>
        <Card t={t}>
          {[
            { label: "Estudiante", pct: 92, color: t.accent, count: 131 },
            { label: "Docente", pct: 7, color: t.accentText, count: 9 },
            { label: "Administrador", pct: 2, color: t.alert, count: 2 },
          ].map(row => (
            <div key={row.label} style={{ display: "flex", alignItems: "center", gap: 10, padding: "9px 0", borderBottom: `1px solid ${t.border}`, fontSize: 14 }}>
              <span style={{ flex: 1 }}>{row.label}</span>
              <div style={{ width: 80, height: 6, borderRadius: 999, background: t.surfaceHigh, overflow: "hidden" }}>
                <div style={{ width: `${row.pct}%`, height: "100%", background: row.color }} />
              </div>
              <span style={{ fontFamily: "ui-monospace,monospace", fontSize: 12, color: t.textSecondary, width: 28, textAlign: "right" }}>{row.count}</span>
            </div>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Últimos registros</div>
        <Card t={t} tight>
          <Item t={t} compact onClick={() => nav("J2")}><Ic id="person" style={{ color: t.textSecondary, width: 18, height: 18 }} /><span style={{ flex: 1, fontSize: 15 }}>dgonzalez@est.una.ac.cr</span><span style={{ fontSize: 12, color: t.textSecondary }}>hace 2 h</span></Item>
          <Item t={t} compact onClick={() => nav("J2")}><Ic id="person" style={{ color: t.textSecondary, width: 18, height: 18 }} /><span style={{ flex: 1, fontSize: 15 }}>fugalde@est.una.ac.cr</span><span style={{ fontSize: 12, color: t.textSecondary }}>hace 5 h</span></Item>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── J1 Usuarios admin ─────────────────────────────────────────────────────────
function ScreenJ1({ t, nav }: { t: Tok; nav: Nav }) {
  const users = [
    { initials: "MG", name: "Maikol Guzmán", email: "maikol.guzman@una.cr", role: "DOCENTE", active: true },
    { initials: "BB", name: "Brandon Brenes", email: "bbrenes@est.una.ac.cr", role: "ESTUDIANTE", active: true },
    { initials: "DG", name: "David González", email: "dgonzalez@est.una.ac.cr", role: "ESTUDIANTE", active: true },
    { initials: "FU", name: "Felipe Ugalde", email: "fugalde@est.una.ac.cr", role: "ESTUDIANTE", active: true },
    { initials: "KJ", name: "Keneth Jara", email: "kjara@est.una.ac.cr", role: "INACTIVO", active: false },
  ]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B4")}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 22, fontWeight: 500, letterSpacing: "-.2px", paddingLeft: 4 }}>Usuarios</span>
        <span style={{ flex: 1 }} />
        <Tap t={t}><Ic id="search" /></Tap>
        <Tap t={t}><Ic id="add" /></Tap>
      </AppBar>
      <div style={{ padding: "10px 16px", display: "flex", gap: 8, borderBottom: `1px solid ${t.border}`, flexShrink: 0 }}>
        {[{ l: "Todos · 142", active: true }, { l: "Docentes · 9", active: false }, { l: "Inactivos · 3", active: false }].map(chip => (
          <span key={chip.l} style={{ padding: "5px 12px", borderRadius: 999, fontSize: 12, background: chip.active ? t.accentSoft : t.neutralSoft, color: chip.active ? t.accentText : t.textSecondary, cursor: "pointer" }}>{chip.l}</span>
        ))}
      </div>
      <Body t={t} scroll style={{ padding: 0 }}>
        {users.map(u => (
          <div key={u.email} onClick={() => nav("J2")} style={{ display: "flex", alignItems: "center", gap: 14, padding: "12px 16px", borderBottom: `1px solid ${t.border}`, minHeight: 72, opacity: u.active ? 1 : .55, cursor: "pointer" }}>
            <div style={{ width: 40, height: 40, borderRadius: 999, background: u.active ? t.accentSoft : t.surfaceHigh, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 14, fontWeight: 600, color: u.active ? t.accentText : t.textSecondary, flexShrink: 0 }}>{u.initials}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16 }}>{u.name}</div>
              <div style={{ fontSize: 14, color: t.textSecondary, marginTop: 2 }}>{u.email}</div>
            </div>
            <Badge t={t} bg={u.role === "INACTIVO" ? t.alertSoft : t.accentSoft} color={u.role === "INACTIVO" ? t.alert : t.accentText}>{u.role}</Badge>
          </div>
        ))}
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── J2 Detalle usuario ────────────────────────────────────────────────────────
function ScreenJ2({ t, nav }: { t: Tok; nav: Nav }) {
  const privs = ["note:create", "note:read:own", "coverage:read:own", "study:attempt", "course:join", "glossary:suggest"]
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("J1")}><Ic id="back" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>David González</span>
        <span style={{ flex: 1 }} />
        <span style={{ color: t.accentText, fontSize: 16, fontWeight: 500, paddingRight: 10, cursor: "pointer" }}>Guardar</span>
      </AppBar>
      <Body t={t} scroll>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "4px 0 18px" }}>
          <div style={{ width: 72, height: 72, borderRadius: 999, background: t.surfaceHigh, border: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24, fontWeight: 600, color: t.accentText }}>DG</div>
          <div style={{ fontSize: 14, color: t.textSecondary, marginTop: 12 }}>dgonzalez@est.una.ac.cr</div>
          <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4 }}>Registrado el 29 de julio de 2026</div>
        </div>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", marginBottom: 8 }}>Roles asignados</div>
        <Card t={t} tight>
          {[
            { label: "Estudiante", sub: "6 privilegios", on: true },
            { label: "Docente", sub: "9 privilegios", on: false },
            { label: "Administrador", sub: "14 privilegios", on: false },
          ].map(r => (
            <Item t={t} compact key={r.label}>
              <div style={{ width: 20, height: 20, borderRadius: 4, border: `2px solid ${r.on ? t.accent : t.textSecondary}`, background: r.on ? t.accent : "transparent", color: t.onAccent, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, cursor: "pointer" }}>
                {r.on && <Ic id="check" style={{ width: 14, height: 14 }} />}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15 }}>{r.label}</div>
                <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 2 }}>{r.sub}</div>
              </div>
            </Item>
          ))}
        </Card>
        <div style={{ fontSize: 11, letterSpacing: ".6px", fontWeight: 600, color: t.textSecondary, textTransform: "uppercase", margin: "18px 0 8px" }}>Privilegios efectivos</div>
        <Card t={t}>
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap" as const }}>
            {privs.map(p => <Badge key={p} t={t} bg={t.neutralSoft} color={t.textSecondary}>{p}</Badge>)}
          </div>
          <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 12, lineHeight: 1.5 }}>
            Se calculan desde <span style={{ fontFamily: "ui-monospace,monospace" }}>user_roles</span> y <span style={{ fontFamily: "ui-monospace,monospace" }}>role_privileges</span>. No se asignan uno por uno.
          </div>
        </Card>
        <Btn t={t} variant="dan">Desactivar la cuenta</Btn>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── J3 Roles y privilegios ────────────────────────────────────────────────────
function ScreenJ3({ t, nav }: { t: Tok; nav: Nav }) {
  return (
    <>
      <SysBar t={t} />
      <AppBar t={t} left={<Tap t={t} onClick={() => nav("B4")}><Ic id="menu" /></Tap>}>
        <span style={{ fontSize: 18, fontWeight: 500, paddingLeft: 4 }}>Roles</span>
        <span style={{ flex: 1 }} />
        <Tap t={t}><Ic id="add" /></Tap>
      </AppBar>
      <Body t={t} scroll>
        {[
          {
            name: "Estudiante", count: "131 USUARIOS", countBg: t.accentSoft, countC: t.accentText,
            desc: "Captura, revisa y estudia sobre sus propios apuntes.",
            privs: ["note:create", "note:read:own", "coverage:read:own", "+3"],
          },
          {
            name: "Docente", count: "9 USUARIOS", countBg: t.accentSoft, countC: t.accentText,
            desc: "Publica temario y glosario; ve brechas agregadas del grupo.",
            privs: ["course:create", "syllabus:publish", "glossary:write", "+6"],
          },
          {
            name: "Administrador", count: "2 USUARIOS", countBg: t.alertSoft, countC: t.alert,
            desc: "Usuarios, roles y estado del sistema.",
            privs: ["user:manage", "role:assign", "system:read", "+11"],
          },
        ].map(role => (
          <Card t={t} key={role.name}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div style={{ fontSize: 18, fontWeight: 500 }}>{role.name}</div>
              <Badge t={t} bg={role.countBg} color={role.countC}>{role.count}</Badge>
            </div>
            <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 8 }}>{role.desc}</div>
            <div style={{ display: "flex", gap: 6, flexWrap: "wrap" as const, marginTop: 12 }}>
              {role.privs.map(p => <Badge key={p} t={t} bg={t.neutralSoft} color={t.textSecondary}>{p}</Badge>)}
            </div>
          </Card>
        ))}
        <Card t={t} style={{ background: t.accentFaint, borderColor: t.accentLine }}>
          <div style={{ display: "flex", gap: 10, alignItems: "flex-start" }}>
            <Ic id="shield" style={{ color: t.accentText, width: 20, height: 20 }} />
            <div>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Ningún rol lee apuntes ajenos</div>
              <div style={{ fontSize: 12, color: t.textSecondary, marginTop: 4, lineHeight: 1.5 }}>
                No existe el privilegio <span style={{ fontFamily: "ui-monospace,monospace" }}>note:read:any</span>. El docente solo ve datos agregados.
              </div>
            </div>
          </div>
        </Card>
      </Body>
      <GestureBar t={t} />
    </>
  )
}

// ── screen registry ──────────────────────────────────────────────────────────
const SCREENS: Record<string, (props: { t: Tok; nav: Nav }) => React.ReactElement> = {
  A1: ScreenA1, A2: ScreenA2, A3: ScreenA3, A4: ScreenA4,
  B1a: ScreenB1a, B1b: ScreenB1b, B1c: ScreenB1c,
  B2: ScreenB2, B3: ScreenB3, B4: ScreenB4,
  C1: ScreenC1, C2: ScreenC2, C2b: ScreenC2b, C3: ScreenC3,
  D1: ScreenD1, D2: ScreenD2, D3: ScreenD3,
  E1: ScreenE1, E2: ScreenE2, E3: ScreenE3, E4: ScreenE4, E5: ScreenE5,
  F1: ScreenF1, F2: ScreenF2, F3: ScreenF3,
  G1: ScreenG1, G2: ScreenG2, G3: ScreenG3, G4: ScreenG4,
  H1: ScreenH1, H2: ScreenH2,
  I1: ScreenI1, I2: ScreenI2, I3: ScreenI3, I4: ScreenI4, I5: ScreenI5,
  J1: ScreenJ1, J2: ScreenJ2, J3: ScreenJ3,
  K1: ScreenK1, K2: ScreenK2,
}

const MAP_GROUPS = [
  { name: "A — Acceso", items: ["A1","A2","A3","A4"] },
  { name: "B — Navegación / Inicio", items: ["B1a","B1b","B1c","B2","B3","B4"] },
  { name: "C — Cursos", items: ["C1","C2","C2b","C3"] },
  { name: "D — Captura", items: ["D1","D2","D3"] },
  { name: "E — Apuntes", items: ["E1","E2","E3","E4","E5"] },
  { name: "F — Cobertura", items: ["F1","F2","F3"] },
  { name: "G — Estudio", items: ["G1","G2","G3","G4"] },
  { name: "H — Sync / IA", items: ["H1","H2"] },
  { name: "I — Docente", items: ["I1","I2","I3","I4","I5"] },
  { name: "J — Administración", items: ["J1","J2","J3"] },
  { name: "K — Perfil", items: ["K1","K2"] },
]

const SCREEN_LABELS: Record<string, string> = {
  A1: "Splash", A2: "Login", A3: "Registro", A4: "Recuperar",
  B1a: "Menú", B1b: "Menú", B1c: "Menú", B2: "Inicio estudiante", B3: "Inicio docente", B4: "Inicio administrador",
  C1: "Mis cursos", C2: "Unirse a curso", C2b: "Conmutador de curso/rol", C3: "Centro de curso",
  D1: "Cámara", D2: "Rechazo / diagnóstico", D3: "Procesamiento",
  E1: "Apuntes", E2: "Mapa de confianza", E3: "Corregir", E4: "Visor de fórmula", E5: "Procedencia",
  F1: "Cobertura", F2: "Detalle de tema", F3: "Delta",
  G1: "Estudio", G2: "Flashcards", G3: "Quiz", G4: "Resultado",
  H1: "Cola de sincronización", H2: "Consumo de IA",
  I1: "Cursos docente", I2: "Nuevo curso", I3: "Publicar temario", I4: "Glosario", I5: "Brechas del grupo",
  J1: "Usuarios", J2: "Detalle usuario", J3: "Roles",
  K1: "Ajustes", K2: "Perfil",
}

// ── main app ─────────────────────────────────────────────────────────────────
export default function App() {
  const [screen, setScreen] = useState("A1")
  const [isNight, setIsNight] = useState(true)
  const t = isNight ? NIGHT : DAY

  const nav = (s: string) => {
    if (s === "B2" || s === "B3" || s === "B4") roleHome = s
    setScreen(s)
  }
  const Screen = SCREENS[screen] ?? SCREENS["A1"]

  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "#0C1219", fontFamily: "Inter, -apple-system, 'Segoe UI', sans-serif" }}>
      <Defs />

      {/* sidebar */}
      <div style={{ width: 260, flexShrink: 0, padding: "24px 20px 40px", position: "sticky", top: 0, height: "100vh", overflowY: "auto", borderRight: "1px solid #1E2A36" }}>
        <div style={{ fontSize: 10, letterSpacing: "1.4px", textTransform: "uppercase", color: "#FFD372", fontWeight: 600, marginBottom: 7 }}>Glifo · prototipo</div>
        <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: "-.3px", color: "#D7D1B9", marginBottom: 4 }}>Mapa de flujo</div>
        <div style={{ fontSize: 11, color: "#666", lineHeight: 1.5, marginBottom: 14 }}>Tocá una pantalla para navegar.</div>

        {/* mode toggle */}
        <div style={{ display: "flex", gap: 8, marginBottom: 14 }}>
          <div onClick={() => setIsNight(n => !n)} style={{ flex: 1, height: 36, borderRadius: 7, border: "1px solid #4A5A6E", background: "#1A222C", color: "#D7D1B9", display: "flex", alignItems: "center", justifyContent: "center", gap: 6, fontSize: 12, fontWeight: 500, cursor: "pointer" }}>
            {isNight ? "🌙 Noche" : "☀️ Día"}
          </div>
          <div onClick={() => setScreen("A1")} style={{ width: 36, height: 36, borderRadius: 7, border: "1px solid #4A5A6E", background: "#1A222C", color: "#666", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", fontSize: 14 }} title="Reiniciar">↺</div>
        </div>

        {/* current screen badge */}
        <div style={{ display: "flex", alignItems: "center", gap: 7, padding: "8px 10px", borderRadius: 7, background: "rgba(255,211,114,.08)", border: "1px solid rgba(255,211,114,.22)", marginBottom: 14 }}>
          <svg width="14" height="14" fill="none" stroke="#FFD372" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"><use href="#person" /></svg>
          <span style={{ fontSize: 11.5, color: "#D7D1B9", fontWeight: 500, flex: 1 }}>{SCREEN_LABELS[screen] ?? screen}</span>
          <span style={{ fontSize: 10, fontFamily: "ui-monospace,monospace", color: "#666" }}>{screen}</span>
        </div>

        {/* groups */}
        {MAP_GROUPS.map(g => (
          <div key={g.name} style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 9, letterSpacing: "1px", textTransform: "uppercase", color: "#555", fontWeight: 600, paddingBottom: 5, borderBottom: "1px solid #1E2A36", marginBottom: 5 }}>{g.name}</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
              {g.items.map(id => (
                <div key={id} onClick={() => nav(id)} title={SCREEN_LABELS[id]} style={{ display: "flex", alignItems: "center", gap: 4, padding: "4px 7px", borderRadius: 5, cursor: "pointer", border: `1px solid ${screen === id ? "#FFD372" : "#2E3B4B"}`, background: screen === id ? "rgba(255,211,114,.12)" : "#141B24", color: screen === id ? "#FFD372" : "#8a9ab0" }}>
                  <b style={{ fontFamily: "ui-monospace,monospace", fontSize: 9.5, flexShrink: 0 }}>{id}</b>
                  <span style={{ fontSize: 9.5, opacity: .75, maxWidth: 90, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{SCREEN_LABELS[id]}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* phone */}
      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: 40 }}>
        <div style={{ position: "relative" }}>
          {/* Android device shell */}
          <div style={{ width: 376, height: 836, background: "#111", borderRadius: 28, padding: "8px 8px", boxShadow: "0 0 0 1px #2E3B4B, 0 28px 60px rgba(0,0,0,.7)", position: "relative" }}>
            {/* camera + speaker */}
            <div style={{ position: "absolute", top: 14, left: "50%", transform: "translateX(-50%)", width: 60, height: 6, background: "#000", borderRadius: 999, display: "flex", alignItems: "center", justifyContent: "center", gap: 8, zIndex: 20 }}>
              <div style={{ width: 6, height: 6, borderRadius: 999, background: "#1a1a1a", border: "1px solid #333" }} />
            </div>

            {/* screen content — 360×800 */}
            <div style={{ width: 360, height: 800, background: t.background, color: t.textPrimary, borderRadius: 20, overflow: "hidden", display: "flex", flexDirection: "column", position: "relative" }}>
              <Screen t={t} nav={nav} />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
