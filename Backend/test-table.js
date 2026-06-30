const dotenv = require('dotenv');
const { createClient } = require('@supabase/supabase-js');

dotenv.config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

async function run() {
  const { data, error } = await supabase
    .from('offline_messages')
    .select('*')
    .limit(1);

  if (error) {
    console.error('❌ Table error:', error);
  } else {
    console.log('✅ Table offline_messages exists! Data:', data);
  }
}

run();
