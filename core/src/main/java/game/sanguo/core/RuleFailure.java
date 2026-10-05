package game.sanguo.core;

/** Stable machine reason and input field, separate from the player's explanation. */
public final class RuleFailure {
    public final String code,field,detail;
    public RuleFailure(String code,String field,String detail){this.code=code;this.field=field;this.detail=detail;}
}
