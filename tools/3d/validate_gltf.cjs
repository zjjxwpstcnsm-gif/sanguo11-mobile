const fs=require('fs'),path=require('path'),validator=require('gltf-validator');
function files(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(dir,e.name)):e.name.endsWith('.glb')?[path.join(dir,e.name)]:[]);}
(async()=>{let errors=0,warnings=0,count=0;const details=[];
for(const family of ['sites','field'])for(const p of files(path.join(__dirname,'../../app/src/main/assets/3d',family)).sort()){
 const file=path.relative(path.join(__dirname,'../../app/src/main/assets/3d',family),p);
 const r=await validator.validateBytes(new Uint8Array(fs.readFileSync(p)),{uri:file,maxIssues:100,externalResourceFunction:()=>Promise.reject(new Error('external resources forbidden'))});
 errors+=r.issues.numErrors;warnings+=r.issues.numWarnings;count++;
 details.push({asset:family+'/'+file,issues:r.issues});
}
fs.mkdirSync(path.join(__dirname,'../../out/r06'),{recursive:true});
fs.writeFileSync(path.join(__dirname,'../../out/r06/khronos.json'),JSON.stringify({version:validator.version(),count,errors,warnings,details},null,2));
console.log({validator:validator.version(),count,errors,warnings});if(errors)process.exitCode=1;
})().catch(e=>{console.error(e);process.exitCode=1;});
