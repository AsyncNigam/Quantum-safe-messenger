const dotenv = require('dotenv');
const { createClient } = require('@supabase/supabase-js');

dotenv.config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
  console.error('❌ Missing Supabase credentials in .env file');
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

async function run() {
  const dummyUser = {
    fingerprint: 'a'.repeat(64),
    ml_kem_public_key: 'b'.repeat(1580),
    x25519_public_key: 'c'.repeat(60),
    deleted_at: null
  };

  console.log('Inserting dummy user...');
  const { data, error } = await supabase
    .from('users')
    .upsert(dummyUser, { onConflict: 'fingerprint' });

  if (error) {
    console.error('❌ Error upserting user:', error);
  } else {
    console.log('✅ User upserted successfully!', data);
  }
}

run();
