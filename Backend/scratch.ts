import Redis from 'ioredis';
const redis = new Redis();
const pipeline = redis.multi();
pipeline.lrangeBuffer('key', 0, -1);
pipeline.del('key');
