package game.sanguo.mobile;

import game.sanguo.api.StateToken;

/** Already-selected original source and immutable committed parent. Does not decide success/speaker/action. */
final class PcVoiceDirective {
    enum Selector { FEEDBACK_A, FEEDBACK_B, ABILITY }
    final StateToken state;
    final String id,parentId,presentationParentId;
    final PortraitMediaIdentity speaker;
    final int voiceTypeRaw,nativeProfile,sideRaw,priority,nativeVoiceId;
    final boolean alternateRaw;
    final Selector selector;
    PcVoiceDirective(StateToken state,String id,String parentId,String presentationParentId,PortraitMediaIdentity speaker,
                     int voiceTypeRaw,int nativeProfile,int sideRaw,boolean alternateRaw,int priority){
        this(state,id,parentId,presentationParentId,speaker,voiceTypeRaw,nativeProfile,sideRaw,alternateRaw,priority,Selector.FEEDBACK_A,null);
    }
    PcVoiceDirective(StateToken state,String id,String parentId,String presentationParentId,PortraitMediaIdentity speaker,
                     int voiceTypeRaw,int nativeProfile,int sideRaw,boolean alternateRaw,int priority,Selector selector,int[] currentAbilityBytes){
        if(state==null||id==null||id.isEmpty()||parentId==null||parentId.isEmpty()||presentationParentId==null||presentationParentId.isEmpty()
            ||speaker==null||priority<0||priority>3)throw new IllegalArgumentException("Incomplete source voice directive");
        int selected;
        if(selector==Selector.FEEDBACK_A)selected=PcVoicePolicy.feedbackAVoice(nativeProfile,voiceTypeRaw,true,sideRaw);
        else if(selector==Selector.FEEDBACK_B)selected=PcVoicePolicy.feedbackBVoice(nativeProfile,voiceTypeRaw,true,sideRaw);
        else if(selector==Selector.ABILITY&&currentAbilityBytes!=null&&currentAbilityBytes.length==4)selected=PcVoicePolicy.abilityVoice(nativeProfile,voiceTypeRaw,true,currentAbilityBytes[0],currentAbilityBytes[1],currentAbilityBytes[2],currentAbilityBytes[3]);
        else throw new IllegalArgumentException("Missing already-calculated source abilities/selector");
        if(selected<0)throw new IllegalArgumentException("Unverified original voice selector input");
        this.state=state;this.id=id;this.parentId=parentId;this.presentationParentId=presentationParentId;this.speaker=speaker;
        this.voiceTypeRaw=voiceTypeRaw;this.nativeProfile=nativeProfile;this.sideRaw=sideRaw;this.alternateRaw=alternateRaw;this.priority=priority;nativeVoiceId=selected;
        this.selector=selector;
    }
}
