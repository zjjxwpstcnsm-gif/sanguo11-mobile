package game.sanguo.api;
import java.util.*;
/** Detached source menu facts. A saved value is never a new-game default. */
public final class PcOpeningOptionsSnapshot {
 public static final class Choice {
  public final int value,controlId;public final String label,rawHex;
  public Choice(int value,String label,String rawHex,int control){this.value=value;this.label=Objects.requireNonNull(label);this.rawHex=Objects.requireNonNull(rawHex);controlId=control;}
 }
 public static final class Group {
  public final String id,title;public final List<Choice>choices;public final Integer savedValue;public final String overrideLabel;
  /** A verified source constraint, never an inferred default or saved choice. */
  public final Integer fixedMenuValue;
  public Group(String id,String title,List<Choice>choices,Integer saved){this(id,title,choices,saved,null);}
  public Group(String id,String title,List<Choice>choices,Integer saved,String overrideLabel){this(id,title,choices,saved,overrideLabel,null);}
  public Group(String id,String title,List<Choice>choices,Integer saved,String overrideLabel,Integer fixedMenuValue){this.id=Objects.requireNonNull(id);this.title=Objects.requireNonNull(title);this.choices=List.copyOf(choices);savedValue=saved;this.overrideLabel=overrideLabel;this.fixedMenuValue=fixedMenuValue;}
  public String savedLabel(){if(savedValue==null)return "未设置";if(overrideLabel!=null)return overrideLabel;return choices.stream().filter(c->c.value==savedValue).map(c->c.label).findFirst().orElse("待核实");}
 }
 public final StateToken state;public final boolean supported,defaultsKnown,automaticOverridesKnown;
 public final String executableSha,receiptSha;public final List<Group>groups;
 public final Integer sourceFlag18;
 public final String previewSourceId;
 public PcOpeningOptionsSnapshot(StateToken state,boolean supported,String exe,String receipt,List<Group>groups){this(state,supported,exe,receipt,groups,null);}
 public PcOpeningOptionsSnapshot(StateToken state,boolean supported,String exe,String receipt,List<Group>groups,Integer sourceFlag){this(state,supported,exe,receipt,groups,sourceFlag,"");}
 public PcOpeningOptionsSnapshot(StateToken state,boolean supported,String exe,String receipt,List<Group>groups,Integer sourceFlag,String previewSource){this.state=Objects.requireNonNull(state);this.supported=supported;defaultsKnown=false;automaticOverridesKnown=sourceFlag!=null;sourceFlag18=sourceFlag;previewSourceId=Objects.requireNonNull(previewSource);executableSha=Objects.requireNonNull(exe);receiptSha=Objects.requireNonNull(receipt);this.groups=List.copyOf(groups);}
 public boolean hasSavedValues(){return groups.stream().allMatch(g->g.savedValue!=null)&&!groups.isEmpty();}
 public static PcOpeningOptionsSnapshot unsupported(StateToken state){return new PcOpeningOptionsSnapshot(state,false,"","",List.of());}
}
