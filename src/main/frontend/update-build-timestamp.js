const fs = require('fs');
const path = require('path');

// Generate current timestamp
const timestamp = new Date().toISOString();

// Update environment files
const envFiles = [
  'src/environments/environment.ts',
  'src/environments/environment.prod.ts'
];

envFiles.forEach(filePath => {
  const fullPath = path.join(__dirname, filePath);
  let content = fs.readFileSync(fullPath, 'utf8');
  
  // Replace the timestamp placeholder with current time
  content = content.replace(
    /buildTimestamp: '[^']*'/,
    `buildTimestamp: '${timestamp}'`
  );
  
  fs.writeFileSync(fullPath, content);
  console.log(`Updated build timestamp in ${filePath} to ${timestamp}`);
});