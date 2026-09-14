package game.sanguo.mobile;

import android.app.AlertDialog;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.IOException;
import java.util.*;

/** Source definitions are deliberately separate from the current world's mutable records. */
final class ContentUi {
    private final MainActivity a; private final World w; private final ClientState state;
    private ContentCatalog catalog;
    private final String[] kinds={"officers","sites","skills","items","scenarios"};
    private final String[] titles={"武将资料","城关港目录","特技目录","宝物目录","剧本缺口"};
    ContentUi(MainActivity a,World w,ClientState state){this.a=a;this.w=w;this.state=state;}
    private TextView text(String value,int size){TextView t=a.text(value,size,a.paper);t.setPadding(a.dp(8),a.dp(5),a.dp(8),a.dp(5));return t;}
    View view(){
        LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);
        try{catalog=ContentCatalog.get();}catch(IOException e){host.addView(text("资料校验失败："+e.getMessage(),18));return host;}
        if(!Arrays.asList(kinds).contains(state.contentKind))state.contentKind="officers";
        host.addView(text("全国资料 / 核验目录",20));
        host.addView(text("资料待原版核验 · 资料不代表规则生效\n官方完整可玩剧本：0",12));
        LinearLayout toolbar=new LinearLayout(a);host.addView(toolbar,new LinearLayout.LayoutParams(-1,a.dp(44)));
        toolbar.addView(a.button(titles[Arrays.asList(kinds).indexOf(state.contentKind)],v->new AlertDialog.Builder(a).setTitle("资料分类").setItems(titles,(d,i)->{state.contentKind=kinds[i];state.contentQuery="";state.contentFirstId="";a.refresh();}).setNegativeButton("取消",null).show()),new LinearLayout.LayoutParams(0,-1,1));
        toolbar.addView(a.button("据点分布预览",v->preview()),new LinearLayout.LayoutParams(0,-1,1));
        EditText search=new EditText(a);search.setSingleLine();search.setTextColor(a.paper);search.setHintTextColor(a.muted);search.setHint("搜索名称或稳定 ID");search.setContentDescription("搜索资料");search.setText(state.contentQuery);host.addView(search,new LinearLayout.LayoutParams(-1,a.dp(40)));
        TextView count=text("",12);host.addView(count);
        FrameLayout frame=new FrameLayout(a);host.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=text("没有符合条件的资料 · 请清空检索",16);empty.setGravity(Gravity.CENTER);frame.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        ListView list=new ListView(a);list.setContentDescription("资料列表");frame.addView(list,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);
        final List<String[]> all=new ArrayList<>();
        if(state.contentKind.equals("officers"))for(ContentCatalog.Officer o:catalog.officers())all.add(new String[]{String.valueOf(o.id),o.name,"统"+o.stat(0)+" 武"+o.stat(1)+" 智"+o.stat(2)+" 政"+o.stat(3)+" 魅"+o.stat(4)+"\n枪戟弩骑兵水 "+o.aptitudeText()+" · "+status(o.status),catalog.alias(o.id)});
        else for(ContentCatalog.Entry e:catalog.rows(state.contentKind))all.add(new String[]{e.id,e.name,subtitle(e),""});
        final List<String[]> shown=new ArrayList<>();
        BaseAdapter adapter=new BaseAdapter(){public int getCount(){return shown.size();}public Object getItem(int p){return shown.get(p);}public long getItemId(int p){return shown.get(p)[0].hashCode();}public boolean hasStableIds(){return true;}
            public View getView(int p,View reuse,ViewGroup parent){TextView row=reuse instanceof TextView?(TextView)reuse:text("",16);String[] r=shown.get(p);row.setText(r[1]+" · ID "+r[0]+"\n"+r[2]);row.setMinimumHeight(a.dp(80));return row;}};
        Runnable filter=()->{shown.clear();String q=state.contentQuery.trim().toLowerCase(Locale.ROOT);for(String[] r:all)if((r[0]+r[1]+r[3]).toLowerCase(Locale.ROOT).contains(q))shown.add(r);count.setText("显示 "+shown.size()+" / "+all.size()+" 条");adapter.notifyDataSetChanged();};
        list.setAdapter(adapter);filter.run();String anchor=state.contentFirstId;for(int i=0;i<shown.size();i++)if(shown.get(i)[0].equals(anchor)){list.setSelection(i);break;}
        list.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int s){}public void onScroll(AbsListView v,int first,int visible,int total){if(first<shown.size()&&visible>0)state.contentFirstId=shown.get(first)[0];}});
        list.setOnItemClickListener((p,v,i,id)->detail(shown.get(i)[0]));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){state.contentQuery=s.toString();state.contentFirstId="";filter.run();list.setSelection(0);}});
        return host;
    }
    private String status(String s){return s.equals("cross-checked")?"能力/适性双表比对（非原版核验）":s.equals("collected")?"已采集 · 待原版核验":"未知 / 未完成";}
    private String subtitle(ContentCatalog.Entry e){List<String> f=e.fields;switch(state.contentKind){case "sites":return (f.get(2).equals("city")?"城市":f.get(2).equals("gate")?"关隘":"港口")+" · "+status(f.get(6));case "scenarios":return f.get(2)+"年"+f.get(3)+"月 · 缺少开局状态，不可选玩";case "items":return f.get(2)+" · 归属剧本未知";default:return "已采集名称 · 效果未接入";}}
    private void detail(String id){
        AlertDialog.Builder dialog=new AlertDialog.Builder(a).setNegativeButton("返回",null);
        if(state.contentKind.equals("officers")){
            ContentCatalog.Officer o=catalog.officer(Integer.parseInt(id));String message="项目 ID "+o.id+" / 来源编号 "+o.sourceId+"\n"+status(o.status)+"\n统率 "+o.stat(0)+" / 武力 "+o.stat(1)+" / 智力 "+o.stat(2)+" / 政治 "+o.stat(3)+" / 魅力 "+o.stat(4)+"\n枪、戟、弩、骑、兵器、水军："+o.aptitudeText()+"\n生年 "+o.birth+" / 卒年 "+o.death+" / 登场 "+o.appearance+"\n特技："+catalog.skillName(o.skillId)+"（"+o.skillId+"）\n关系原文："+o.relationsRaw+"\n\n生卒、登场、关系与本表特技尚未接入规则。只有武将资料演练的 18 人能力/适性用于开局。\n来源："+catalog.sourceUrl(o.source);
            dialog.setTitle(o.name).setMessage("目标：Windows 繁中 PK 1.1（安装哈希未知）\n"+message);
            World.Officer actual=w.officer(o.id);if(w.scenarioId.equals("officer-reference-drill")&&actual!=null)dialog.setPositiveButton("当前武将",(d,n)->a.officerDetail(actual));
        }else{
            ContentCatalog.Entry found=null;for(ContentCatalog.Entry e:catalog.rows(state.contentKind))if(e.id.equals(id))found=e;
            if(found==null)return;ContentCatalog.Entry e=found;String extra;
            switch(state.contentKind){case "sites":extra="来源表 X="+e.fields.get(3)+"，Y="+e.fields.get(4)+"\n坐标方向和六角转换未知；不是游戏地形。45 个关港坐标存在疑点，未绘入预览。\n耐久资料："+e.fields.get(5);break;case "items":extra="价值："+e.fields.get(3)+"\n表内持有人："+e.fields.get(4)+"\n表内所在地："+e.fields.get(5)+"\n所属剧本未知；未加载归属或效果。";break;case "scenarios":extra="缺少原版地形、势力/君主、全部人员身份位置、资源兵装舰船、外交、研究及事件状态。仅有名称和日期资料，不可作为官方开局。";break;default:extra="仅名称清单；原版编号和效果未完成核验。本目录不改变运行中特技。";}
            dialog.setTitle(e.name).setMessage("项目 ID "+e.id+"\n"+subtitle(e)+"\n"+extra+"\n来源："+catalog.sourceUrl(e.fields.get(e.fields.size()-1)));
        }
        dialog.show();
    }
    private void preview(){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.addView(text("42 城来源 X/Y 分布 · 非六角地图\n45 关港坐标隔离；地形、水系、道路、开发地未知。拖动、双指缩放；点城市查看来源。",13));
        View atlas=new View(a){final Paint p=new Paint(3);float zoom=1,dx,dy,lastX,lastY;boolean moved;
            final ScaleGestureDetector scale=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){float old=zoom;zoom=Math.max(1,Math.min(5,zoom*d.getScaleFactor()));dx=d.getFocusX()-(d.getFocusX()-dx)*zoom/old;dy=d.getFocusY()-(d.getFocusY()-dy)*zoom/old;invalidate();return true;}});
            float unit(){return Math.min(getWidth(),getHeight())/210f;}
            protected void onDraw(Canvas c){c.drawColor(0xff162b30);c.save();c.translate(dx,dy);c.scale(zoom,zoom);float u=unit();for(ContentCatalog.Entry e:catalog.rows("sites"))if(e.fields.get(2).equals("city")){float x=(Integer.parseInt(e.fields.get(3))+5)*u,y=(Integer.parseInt(e.fields.get(4))+5)*u;p.setColor(a.gold);c.drawCircle(x,y,3/zoom,p);p.setTextSize(12*a.getResources().getDisplayMetrics().density/zoom);p.setColor(a.paper);c.drawText(e.name,x+4/zoom,y,p);}c.restore();}
            public boolean onTouchEvent(MotionEvent e){scale.onTouchEvent(e);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:lastX=e.getX();lastY=e.getY();moved=false;return true;case MotionEvent.ACTION_POINTER_DOWN:moved=true;return true;case MotionEvent.ACTION_MOVE:if(!scale.isInProgress()&&e.getPointerCount()==1){float x=e.getX()-lastX,y=e.getY()-lastY;if(Math.abs(x)+Math.abs(y)>3)moved=true;dx+=x;dy+=y;invalidate();}lastX=e.getX();lastY=e.getY();return true;case MotionEvent.ACTION_UP:if(!moved){performClick();ContentCatalog.Entry closest=null;float best=a.dp(28);for(ContentCatalog.Entry s:catalog.rows("sites"))if(s.fields.get(2).equals("city")){float x=(Integer.parseInt(s.fields.get(3))+5)*unit()*zoom+dx,y=(Integer.parseInt(s.fields.get(4))+5)*unit()*zoom+dy;float distance=(float)Math.hypot(x-e.getX(),y-e.getY());if(distance<best){best=distance;closest=s;}}if(closest!=null)new AlertDialog.Builder(a).setTitle(closest.name).setMessage("来源 X/Y："+closest.fields.get(3)+", "+closest.fields.get(4)+"\n待原版核验；未用作运行时六角坐标。").setPositiveButton("返回",null).show();}return true;default:return true;}}
            public boolean performClick(){super.performClick();return true;}
        };atlas.setContentDescription("来源城市坐标预览");panel.addView(atlas,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog d=new AlertDialog.Builder(a).setTitle("据点分布预览").setView(panel).setNegativeButton("返回",null).create();d.show();d.getWindow().setLayout(a.getResources().getDisplayMetrics().widthPixels*9/10,a.getResources().getDisplayMetrics().heightPixels*9/10);
    }
}
