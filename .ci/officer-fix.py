from pathlib import Path

def edit(file,before,after):
    p=Path(file);s=p.read_text();assert s.count(before)==1,(file,before[:100]);p.write_text(s.replace(before,after))

activity='app/src/main/java/game/sanguo/mobile/CustomOfficerActivity.java'
pack='app/src/main/java/game/sanguo/mobile/CustomOfficerPack.java'
probe='app/src/androidTest/java/game/sanguo/mobile/CustomOfficerProbe.java'
edit(activity,'private void relationships()throws JSONException','private void relationships()throws IOException,JSONException')
edit(activity,'(d,n)->startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setClipData(ClipData.newRawUri("武将包",uri)),"分享武将数据包"))','(d,n)->{Intent share=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("武将包",uri));startActivity(Intent.createChooser(share,"分享武将数据包"));}')
edit(pack,'''        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){
            entry(zip,"manifest.json",manifest.toString().getBytes(StandardCharsets.UTF_8));for(String name:names)entry(zip,"portraits/"+name,CustomOfficerImages.read(context,name));
        }if(bytes.size()>MAX_PACK)throw new IOException("数据包超过20MiB限制");return bytes.toByteArray();''','''        byte[] json=manifest.toString().getBytes(StandardCharsets.UTF_8);if(json.length>CustomOfficerLibrary.MAX_BYTES)throw new IOException("清单超过16MiB限制");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();OutputStream bounded=new FilterOutputStream(bytes){
            private int size;
            @Override public void write(int b)throws IOException{if(size>=MAX_PACK)throw new IOException("数据包超过20MiB限制");out.write(b);size++;}
            @Override public void write(byte[] b,int off,int length)throws IOException{if(length>MAX_PACK-size)throw new IOException("数据包超过20MiB限制");out.write(b,off,length);size+=length;}
        };
        int unpacked=json.length;try(ZipOutputStream zip=new ZipOutputStream(bounded)){
            entry(zip,"manifest.json",json);for(String name:names){byte[] png=CustomOfficerImages.read(context,name);unpacked+=png.length;if(unpacked>MAX_PACK)throw new IOException("人物与头像合计超过20MiB限制");entry(zip,"portraits/"+name,png);}
        }return bytes.toByteArray();''')
edit(pack,'更新同源会明确替换该源方案；复制模式不合并已有剧本方案。','同源库更新会替换该库方案；其他导入只补充缺少的剧本方案，已存在的同剧本方案保留，须手动添加新人物投放。')
edit(pack,'if(position!=null&&policy==Conflict.SKIP){ids.put(old,old);skipped.add(old);continue;}','if(position!=null&&policy==Conflict.SKIP){JSONObject current=existing.getJSONObject(position);if(!current.optString("origin",next.getString("libraryId")).equals(src.optString("origin",packId))||current.optInt("targetId",-1)!=src.optInt("targetId",-1))throw new IOException("同ID并非同一身份，不能将关系绑定到旧人物；请选择作为新人物导入："+src.getString("name"));ids.put(old,old);skipped.add(old);continue;}')
edit(pack,'        for(JSONObject o:pending){JSONArray links=o.getJSONArray("relationships");','''        Map<String,String> historyCopies=new HashMap<>();Set<String> ambiguous=new HashSet<>();
        if(policy==Conflict.COPY)for(int i=0;i<incoming.length();i++){JSONObject src=incoming.getJSONObject(i);int target=src.optInt("targetId",-1);if(target>=0){String historical="h:"+target;if(historyCopies.put(historical,ids.get(src.getString("id")))!=null)ambiguous.add(historical);}}
        for(JSONObject o:pending){JSONArray links=o.getJSONArray("relationships");''')
edit(pack,'if(ids.containsKey(target))link.put("target",ids.get(target));','if(ids.containsKey(target))link.put("target",ids.get(target));else if(policy==Conflict.COPY&&historyCopies.containsKey(target)){if(ambiguous.contains(target))throw new IOException("包内多个历史覆盖对应同一关系目标，请先移除冲突覆盖再复制导入："+target);link.put("target",historyCopies.get(target));}')
edit(pack,'''        CustomOfficerLibrary.validate(next);List<File> newFiles=new ArrayList<>();try{
            for(Map.Entry<String,byte[]> e:pack.images.entrySet()){File path=CustomOfficerImages.file(c,e.getKey());if(!path.exists()){CustomOfficerImages.store(c,e.getValue());newFiles.add(path);}}
            library.replace(next);
        }catch(IOException|JSONException|RuntimeException e){for(File f:newFiles)f.delete();throw e;}''','''        CustomOfficerLibrary.validate(next);
        // Content-addressed assets are safe to retain if the visible atomic library commit fails.
        // Never delete a promoted asset: a concurrent writer or an existing campaign may share it.
        for(Map.Entry<String,byte[]> e:pack.images.entrySet()){File path=CustomOfficerImages.file(c,e.getKey());if(!path.exists())CustomOfficerImages.store(c,e.getValue());}
        library.replace(next);''')
edit(probe,'World.Officer enemyOfficer=commands.officers.stream().filter(o->o.owner==1&&o.unitId<0&&o.role!=Strategy.Role.RULER).findFirst().orElseThrow();','World.Officer enemyOfficer=new World.Officer(990001,"测试守军",1,-1,70,70,70,70,70);commands.officers.add(enemyOfficer);')
edit(probe,'    void run(String mode)throws Exception{','''    void run(String mode)throws Exception{
        try{runChecks(mode);}catch(Exception|AssertionError e){
            try{shot("failure-"+mode);try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(directory(),"failure.txt")),"UTF-8")){out.write(report.toString()+"\\n"+e);}}catch(Exception ignored){}
            throw e;
        }
    }
    private void runChecks(String mode)throws Exception{''')
