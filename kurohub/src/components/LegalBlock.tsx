import { m } from "framer-motion";

export function LegalBlock() {
  const lastUpdated = "August 1, 2026";
  const effectiveDate = "August 1, 2026";
  const contactEmail = "legal@kurostream.tv";
  const dmcaEmail = "dmca@kurostream.tv";

  return (
    <m.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className="glass-card p-8 md:p-10 max-w-4xl mx-auto"
    >
      <div className="mb-8 pb-6 border-b border-border/30">
        <h2 className="font-display text-3xl font-bold mb-2">Legal Documents</h2>
        <p className="text-sm text-muted-foreground">
          Last updated: {lastUpdated} | Effective: {effectiveDate}
        </p>
      </div>

      <div className="space-y-12 text-sm text-muted-foreground leading-relaxed">
        {/* Privacy Policy */}
        <section id="privacy" className="space-y-4">
          <h3 className="font-display text-xl font-bold text-foreground">Privacy Policy</h3>

          <h4 className="font-semibold text-foreground">1. Data We Collect</h4>
          <p>
            KuroStream is designed with privacy as a core principle. We collect only the minimum
            data necessary to provide the service:
          </p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>
              <strong>Account data:</strong> Email, display name, and profile photo (via Firebase
              Auth)
            </li>
            <li>
              <strong>Purchase history:</strong> Item IDs, amounts, and timestamps for entitlement
              verification
            </li>
            <li>
              <strong>Active skin preference:</strong> Your selected skin ID for cross-device sync
            </li>
            <li>
              <strong>API usage:</strong> Rate-limited request logs (IP, endpoint, timestamp) for
              abuse prevention
            </li>
          </ul>
          <p>
            We do <strong>not</strong> collect: watch history, media library contents, playback
            position, search queries, device identifiers beyond what Firebase requires, or any
            behavioral telemetry.
          </p>

          <h4 className="font-semibold text-foreground">2. How We Use Your Data</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Authenticate and identify your account</li>
            <li>Sync purchases and active skin across your devices</li>
            <li>Enforce rate limits and prevent abuse</li>
            <li>Process DMCA/legal requests</li>
            <li>Comply with legal obligations</li>
          </ul>

          <h4 className="font-semibold text-foreground">3. Data Storage & Retention</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Account data: Stored in your Firebase project (you control retention)</li>
            <li>
              Purchase/skin data: Stored in Cloudflare D1 (our database), retained while account
              exists
            </li>
            <li>Rate limit logs: Stored in Cloudflare KV, auto-expire after 1 hour</li>
            <li>Submission reviews: Retained for 2 years for audit purposes</li>
          </ul>

          <h4 className="font-semibold text-foreground">4. Third-Party Services</h4>
          <p>We use the following processors:</p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>
              <strong>Firebase (Google):</strong> Authentication only — see{" "}
              <a
                href="https://policies.google.com/privacy"
                target="_blank"
                rel="noopener"
                className="text-primary hover:underline"
              >
                Google Privacy Policy
              </a>
            </li>
            <li>
              <strong>Cloudflare:</strong> D1 database, KV storage, Workers hosting — see{" "}
              <a
                href="https://www.cloudflare.com/privacypolicy/"
                target="_blank"
                rel="noopener"
                className="text-primary hover:underline"
              >
                Cloudflare Privacy Policy
              </a>
            </li>
          </ul>
          <p>We do not sell, rent, or share your personal data with any other third parties.</p>

          <h4 className="font-semibold text-foreground">5. Your Rights</h4>
          <p>Under GDPR, CCPA, and similar laws, you have the right to:</p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Access your data</li>
            <li>Rectify inaccurate data</li>
            <li>Request deletion (account deletion via Firebase console)</li>
            <li>Export your data (contact us)</li>
            <li>Object to processing</li>
          </ul>
          <p>
            Contact{" "}
            <a href={`mailto:${contactEmail}`} className="text-primary hover:underline">
              {contactEmail}
            </a>{" "}
            to exercise these rights.
          </p>

          <h4 className="font-semibold text-foreground">6. Children's Privacy</h4>
          <p>
            KuroStream is not directed at children under 13 (or 16 in EU). We do not knowingly
            collect data from children.
          </p>

          <h4 className="font-semibold text-foreground">7. Changes</h4>
          <p>
            We may update this policy. Material changes will be announced via the app/website and
            email (if provided).
          </p>
        </section>

        {/* Terms of Service */}
        <section id="terms" className="space-y-4 border-t border-border/30 pt-8">
          <h3 className="font-display text-xl font-bold text-foreground">Terms of Service</h3>

          <h4 className="font-semibold text-foreground">1. Acceptance</h4>
          <p>
            By accessing or using KuroStream (the "Service"), you agree to these Terms. If you
            disagree, do not use the Service.
          </p>

          <h4 className="font-semibold text-foreground">2. Description of Service</h4>
          <p>
            KuroStream is a self-hosted, open-source media player application. The Service includes:
          </p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>The KuroStream application (Android TV, mobile, desktop)</li>
            <li>The KuroStream website and marketplace (kurostream.tv)</li>
            <li>API services for skin/extension sync and marketplace</li>
          </ul>
          <p>
            <strong>
              KuroStream does not host, stream, or provide access to any copyrighted video content.
            </strong>{" "}
            It plays media from your local files, personal cloud storage, and licensed APIs you
            configure.
          </p>

          <h4 className="font-semibold text-foreground">3. User Accounts</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>You must provide accurate registration information</li>
            <li>You are responsible for securing your credentials</li>
            <li>One account per person; no sharing or resale</li>
            <li>We may suspend accounts for Terms violations</li>
          </ul>

          <h4 className="font-semibold text-foreground">4. Marketplace & Purchases</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>All prices in USD; taxes added where applicable</li>
            <li>Purchases grant a personal, non-transferable license to use the skin/extension</li>
            <li>Skins Pass: Lifetime access to all current and future premium skins</li>
            <li>Refunds: Digital goods are non-refundable except where required by law</li>
            <li>Platform commission: 15% (sellers receive 85%)</li>
          </ul>

          <h4 className="font-semibold text-foreground">5. Acceptable Use</h4>
          <p>
            You agree <strong>not</strong> to:
          </p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Use the Service for any illegal purpose</li>
            <li>Upload, distribute, or facilitate access to pirated/unlicensed content</li>
            <li>Reverse engineer, decompile, or extract source code from compiled extensions</li>
            <li>Interfere with the Service's security or rate-limiting measures</li>
            <li>Impersonate others or misrepresent affiliation</li>
            <li>Submit extensions/skins violating the Acceptable Use Policy (see below)</li>
          </ul>

          <h4 className="font-semibold text-foreground">6. Intellectual Property</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>KuroStream code: GPL-3.0 (see GitHub)</li>
            <li>Marketplace items: Licensed per listing (original or "Inspired" designs)</li>
            <li>You retain ownership of your submissions; you grant us a license to distribute</li>
            <li>We respect third-party IP — see DMCA Policy below</li>
          </ul>

          <h4 className="font-semibold text-foreground">
            7. Disclaimers & Limitation of Liability
          </h4>
          <p>
            THE SERVICE IS PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND. TO THE MAXIMUM EXTENT
            PERMITTED BY LAW, WE ARE NOT LIABLE FOR INDIRECT, INCIDENTAL, SPECIAL, OR CONSEQUENTIAL
            DAMAGES.
          </p>

          <h4 className="font-semibold text-foreground">8. Termination</h4>
          <p>
            We may terminate your access for material breach. Upon termination, your license to
            purchased items ends, but locally installed skins/extensions continue to function.
          </p>

          <h4 className="font-semibold text-foreground">9. Governing Law</h4>
          <p>
            These Terms are governed by the laws of Delaware, USA, without regard to conflict of
            laws principles.
          </p>
        </section>

        {/* Acceptable Use Policy */}
        <section id="aup" className="space-y-4 border-t border-border/30 pt-8">
          <h3 className="font-display text-xl font-bold text-foreground">
            Acceptable Use Policy (for Extension/Skin Submitters)
          </h3>

          <p className="font-semibold">
            By submitting to the KuroStream Marketplace, you agree to this AUP.
          </p>

          <h4 className="font-semibold text-foreground">1. Prohibited Content</h4>
          <p>Submissions MUST NOT:</p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>
              <strong>Implement or depend on torrent/magnet/P2P/DHT functionality</strong> of any
              kind
            </li>
            <li>
              Source movies, TV shows, anime, sports, or news from unlicensed/unspecified
              third-party sites ("multi-source aggregation," "various sources," etc. without a
              named, licensed API)
            </li>
            <li>
              Reference specific copyrighted characters, franchises, or trademarked brands (e.g.,
              named anime characters, movie titles, network names like "Comedy Central") without
              proof of license
            </li>
            <li>
              Request permissions/scopes not needed for stated function (e.g., a "subtitle"
              extension requesting filesystem or network-proxy access)
            </li>
            <li>
              Collect more user data than necessary (contacts, precise location, full media library
              indexing without on-device-only justification)
            </li>
            <li>Contain malware, spyware, or hidden functionality</li>
            <li>Impersonate official KuroStream extensions or other developers</li>
          </ul>

          <h4 className="font-semibold text-foreground">2. Required Disclosures</h4>
          <p>Every submission MUST include:</p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>
              A <strong>legal_basis</strong> field declaring the legal basis for any content source
              (e.g., "Official TMDB API," "User's own cloud storage," "Public domain / Creative
              Commons")
            </li>
            <li>Acceptance of this AUP via checkbox (timestamped per submission)</li>
            <li>Accurate manifest with only necessary permissions declared</li>
          </ul>

          <h4 className="font-semibold text-foreground">3. Review Process</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Automated pre-screening runs on every submission (keyword/manifest checks)</li>
            <li>
              Flagged items enter manual review queue (status: <code>pending_review</code>)
            </li>
            <li>
              Nothing goes live without <code>status: approved</code>
            </li>
            <li>Auto-rejected items receive specific rejection reasons</li>
          </ul>

          <h4 className="font-semibold text-foreground">4. Post-Publication</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Users can report violations via the Report button</li>
            <li>
              Items crossing a report threshold are auto-hidden (<code>status: hidden</code>)
              pending review
            </li>
            <li>Verified violations result in removal and potential submitter ban</li>
          </ul>

          <h4 className="font-semibold text-foreground">5. Enforcement</h4>
          <p>
            We reserve the right to remove any submission at any time for AUP violations. Repeat
            offenders may be permanently banned from submitting.
          </p>
        </section>

        {/* DMCA / IP Takedown Policy */}
        <section id="dmca" className="space-y-4 border-t border-border/30 pt-8">
          <h3 className="font-display text-xl font-bold text-foreground">
            DMCA / Intellectual Property Takedown Policy
          </h3>

          <h4 className="font-semibold text-foreground">1. Designated Agent</h4>
          <p>
            KuroStream's designated agent for DMCA notifications:
            <br />
            <strong>Email:</strong>{" "}
            <a href={`mailto:${dmcaEmail}`} className="text-primary hover:underline">
              {dmcaEmail}
            </a>
            <br />
            <strong>Subject line:</strong> "DMCA Takedown — KuroStream"
          </p>

          <h4 className="font-semibold text-foreground">2. Notification Requirements</h4>
          <p>To be effective, a DMCA notice must include:</p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Physical or electronic signature of the copyright owner or authorized agent</li>
            <li>Identification of the copyrighted work claimed to be infringed</li>
            <li>
              Identification of the material claimed to be infringing (item ID, URL, or specific
              location in the marketplace)
            </li>
            <li>Contact information (address, phone, email)</li>
            <li>Statement of good faith belief that use is not authorized</li>
            <li>
              Statement under penalty of perjury that information is accurate and you are authorized
              to act
            </li>
          </ul>

          <h4 className="font-semibold text-foreground">3. Takedown SLA</h4>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>
              <strong>Initial response:</strong> Within 24 hours of valid notice receipt
            </li>
            <li>
              <strong>Content removal:</strong> Within 48 hours of validated notice
            </li>
            <li>
              <strong>Submitter notification:</strong> Within 24 hours of removal
            </li>
            <li>
              <strong>Counter-notification window:</strong> 10 business days
            </li>
            <li>
              <strong>Restoration (if valid counter-notification):</strong> Within 14 business days
            </li>
          </ul>

          <h4 className="font-semibold text-foreground">4. Counter-Notification</h4>
          <p>
            If your content was removed, you may submit a counter-notification to{" "}
            <a href={`mailto:${dmcaEmail}`} className="text-primary hover:underline">
              {dmcaEmail}
            </a>{" "}
            including:
          </p>
          <ul className="list-disc list-inside space-y-2 ml-4">
            <li>Your physical/electronic signature</li>
            <li>Identification of removed material and its prior location</li>
            <li>Statement under penalty of perjury that removal was mistaken</li>
            <li>Consent to jurisdiction of your federal district court</li>
            <li>Acceptance of service of process from the original complainant</li>
          </ul>

          <h4 className="font-semibold text-foreground">5. Repeat Infringer Policy</h4>
          <p>Accounts with 3+ validated DMCA takedowns within 12 months will be terminated.</p>

          <h4 className="font-semibold text-foreground">6. Non-DMCA IP Complaints</h4>
          <p>
            Trademark, publicity rights, and other IP complaints should be sent to{" "}
            <a href={`mailto:${contactEmail}`} className="text-primary hover:underline">
              {contactEmail}
            </a>{" "}
            with supporting documentation. We evaluate these on a case-by-case basis.
          </p>
        </section>

        {/* License */}
        <section id="license" className="space-y-4 border-t border-border/30 pt-8">
          <h3 className="font-display text-xl font-bold text-foreground">License</h3>
          <p>
            KuroStream application code is licensed under <strong>GPL-3.0</strong>. Source code is
            available at
            <a
              href="https://github.com/OtakuCompiler/KuroStream"
              target="_blank"
              rel="noopener"
              className="text-primary hover:underline"
            >
              GitHub
            </a>
            .
          </p>
          <p>
            Marketplace items (skins, extensions) have their own licenses as specified in each
            listing. "Inspired" skins are original designs inspired by popular aesthetics — they
            contain no copyrighted assets, character names, or trademarked elements.
          </p>
          <p>
            By submitting to the marketplace, you grant KuroStream a worldwide, non-exclusive,
            royalty-free license to host, distribute, and display your submission.
          </p>
        </section>

        {/* Contact */}
        <section id="contact" className="space-y-4 border-t border-border/30 pt-8">
          <h3 className="font-display text-xl font-bold text-foreground">Contact</h3>
          <p>For legal inquiries, contact:</p>
          <ul className="list-disc list-inside space-y-1 ml-4">
            <li>
              General legal:{" "}
              <a href={`mailto:${contactEmail}`} className="text-primary hover:underline">
                {contactEmail}
              </a>
            </li>
            <li>
              DMCA/IP takedowns:{" "}
              <a href={`mailto:${dmcaEmail}`} className="text-primary hover:underline">
                {dmcaEmail}
              </a>
            </li>
            <li>
              Security vulnerabilities:{" "}
              <a href="mailto:security@kurostream.tv" className="text-primary hover:underline">
                security@kurostream.tv
              </a>
            </li>
          </ul>
        </section>
      </div>
    </m.div>
  );
}
