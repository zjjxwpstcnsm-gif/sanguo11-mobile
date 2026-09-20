package game.sanguo.mobile;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.*;
import game.sanguo.core.*;
import java.util.*;
import java.util.function.Function;

/** A common icon language across the map, lists and command pickers. */
final class GameIcon extends Drawable {
    private final Object item;private final MapModels models=new MapModels();private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int color;
    GameIcon(Object item,int color){this.item=item;this.color=color;}
    static boolean supports(Object item){return item instanceof World.Officer||item instanceof World.City||item instanceof World.Weapon||item instanceof Army.Ship||item instanceof Domestic.Kind||item instanceof War.StructureKind||item instanceof War.Structure||item instanceof Treasures.Definition||item instanceof Treasures.Item;}
    static Drawable drawable(Context context,World w,Object item){
        BuildingAtlas.load(context);VisualAssets.load(context);CityAtlas.load(context);
        if(item instanceof World.Officer)return new OfficerPortrait(context,w,(World.Officer)item);
        return new GameIcon(item,item instanceof World.City?FactionColors.color(w,((World.City)item).owner):item instanceof War.Structure?FactionColors.color(w,((War.Structure)item).owner):item instanceof World.Unit?FactionColors.color(w,((World.Unit)item).owner):FactionColors.color(w,w.player));
    }
    static <T> ArrayAdapter<T> adapter(Context context,World w,List<T> items,Function<T,String> label){
        return new ArrayAdapter<T>(context,android.R.layout.select_dialog_item,items){
            @Override public android.view.View getView(int position,android.view.View reuse,android.view.ViewGroup parent){
                TextView row=(TextView)super.getView(position,reuse,parent);T item=getItem(position);row.setText(label.apply(item));
                int size=Math.round(44*context.getResources().getDisplayMetrics().density);Drawable icon=supports(item)?drawable(context,w,item):null;if(icon!=null)icon.setBounds(0,0,size,size);
                row.setCompoundDrawables(icon,null,null,null);row.setCompoundDrawablePadding(size/4);row.setMinHeight(size+size/4);row.setTextSize(14);return row;
            }
        };
    }
    @Override public void draw(Canvas c){
        Rect b=getBounds();float radius=b.width()*.14f;paint.setColor(0xff20373f);c.drawRoundRect(b.left,b.top,b.right,b.bottom,radius,radius,paint);
        c.save();c.translate(b.exactCenterX(),b.exactCenterY()+b.height()*.10f);c.scale(b.width()/62f,b.height()/62f);
        if(item instanceof Treasures.Item)VisualAssets.draw(c,1,((Treasures.Item)item).definition.kind.ordinal(),51,52,18);
        else if(item instanceof Treasures.Definition)VisualAssets.draw(c,1,((Treasures.Definition)item).kind.ordinal(),51,52,18);
        else if(item instanceof World.City)models.city(c,(World.City)item,color);
        else if(item instanceof World.SiteKind)models.city(c,(World.SiteKind)item,color);
        else if(item instanceof Domestic.Facility)models.facility(c,((Domestic.Facility)item).kind,color);
        else if(item instanceof Domestic.Kind)models.facility(c,(Domestic.Kind)item,color);
        else if(item instanceof War.Structure)models.structure(c,((War.Structure)item).kind,color);
        else if(item instanceof War.StructureKind)models.structure(c,(War.StructureKind)item,color);
        else if(item instanceof World.Weapon)models.weaponIcon(c,(World.Weapon)item,color);
        else if(item instanceof Army.Ship)models.shipIcon(c,(Army.Ship)item,color);
        else if(item instanceof World.Unit)models.weaponIcon(c,((World.Unit)item).weapon,color);
        c.restore();
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
