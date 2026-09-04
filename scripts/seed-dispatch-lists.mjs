/**
 * One-shot seed for admin-managed Pickup/Return lists.
 * Writes companies + delivery partners into Firestore (skips names that already exist).
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=~/.config/firebase/...json node scripts/seed-dispatch-lists.mjs
 */
import { readFileSync } from "fs";
import { homedir } from "os";
import { join } from "path";

const PROJECT_ID = "laiza-6aace";

const COMPANIES = [
  "Amazon",
  "Flipkart",
  "Myntra",
  "Meesho",
  "Snapdeal",
  "Ajio",
  "Nykaa",
  "Other",
];

const PARTNERS = [
  "Amazon Delivery",
  "eKart",
  "BlueDart",
  "Shiprocket",
  "Delhivery",
  "DTDC",
  "Ecom Express",
  "Xpressbees",
  "Shadowfax",
  "India Post",
  "Valmo",
];

function loadAccessToken() {
  const credPath =
    process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    join(homedir(), ".config/firebase/saifsalmani224_gmail_com_application_default_credentials.json");
  const creds = JSON.parse(readFileSync(credPath, "utf8"));
  return { creds, credPath };
}

async function getToken(creds) {
  const body = new URLSearchParams({
    client_id: creds.client_id,
    client_secret: creds.client_secret,
    refresh_token: creds.refresh_token,
    grant_type: "refresh_token",
  });
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) {
    throw new Error(`Token refresh failed: ${res.status} ${await res.text()}`);
  }
  const json = await res.json();
  return json.access_token;
}

async function listCollection(token, collectionId) {
  const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collectionId}?pageSize=300`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status === 404) return [];
  if (!res.ok) {
    throw new Error(`List ${collectionId} failed: ${res.status} ${await res.text()}`);
  }
  const json = await res.json();
  return (json.documents || []).map((doc) => {
    const name = doc.fields?.name?.stringValue?.trim() || "";
    const id = doc.name.split("/").pop();
    return { id, name };
  });
}

async function createDoc(token, collectionId, id, name) {
  const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collectionId}?documentId=${encodeURIComponent(id)}`;
  const now = Date.now();
  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      fields: {
        id: { stringValue: id },
        name: { stringValue: name },
        createdAt: { integerValue: String(now) },
      },
    }),
  });
  if (!res.ok) {
    throw new Error(`Create ${collectionId}/${id} failed: ${res.status} ${await res.text()}`);
  }
}

function uuid() {
  return crypto.randomUUID();
}

async function seedCollection(token, collectionId, names) {
  const existing = await listCollection(token, collectionId);
  const have = new Set(existing.map((e) => e.name.toLowerCase()).filter(Boolean));
  let added = 0;
  let skipped = 0;
  for (const name of names) {
    if (have.has(name.toLowerCase())) {
      skipped += 1;
      continue;
    }
    const id = uuid();
    await createDoc(token, collectionId, id, name);
    have.add(name.toLowerCase());
    added += 1;
    console.log(`  + ${collectionId}: ${name}`);
  }
  console.log(`  ${collectionId}: added ${added}, already present ${skipped}`);
}

async function main() {
  const { creds, credPath } = loadAccessToken();
  console.log(`Using credentials: ${credPath}`);
  console.log(`Project: ${PROJECT_ID}`);
  const token = await getToken(creds);
  console.log("\nSeeding marketplace_companies…");
  await seedCollection(token, "marketplace_companies", COMPANIES);
  console.log("\nSeeding delivery_partners…");
  await seedCollection(token, "delivery_partners", PARTNERS);
  console.log("\nDone. Refresh admin Records → Companies / Delivery partners.");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
