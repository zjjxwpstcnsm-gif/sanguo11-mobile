package game.sanguo.core;
/** Original5ba410 deterministic signed32 command hash, not global RNG.
 * Inputs are supplied by the original caller; no guessed ID/date semantics. */
final class PcCommandRoll {
 static int calculate(int bound,int...inputs){
  if(inputs.length!=7)throw new IllegalArgumentException("Original command hash requires seven inputs");
  if(bound<=1)return 0;
  int remainder=0;for(int input:inputs)remainder=(input*0x6c078965+0x3039+remainder)%bound;
  return Math.max(0,remainder);
 }
 private PcCommandRoll(){}
}
