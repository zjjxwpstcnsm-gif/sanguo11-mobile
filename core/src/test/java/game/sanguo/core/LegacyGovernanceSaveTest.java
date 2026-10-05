package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Genuine v32 writer output must continue identically across a current save/reload. */
public final class LegacyGovernanceSaveTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        byte[] legacy;
        try(InputStream input=LegacyGovernanceSaveTest.class.getResourceAsStream("/save-v32-central-native.sg11")) {
            if(input==null)throw new IOException("historical v32 fixture missing");legacy=input.readAllBytes();
        }
        byte[] original=legacy.clone();
        check(java.nio.ByteBuffer.wrap(legacy).getInt(4)==32,"real historical v32 header");
        World direct=SaveCodec.decode(legacy);
        check(direct.governance.grades.equals(Map.of(0,1,1,4,2,3)),"legacy grades canonicalized to existing earned titles without waiting for a turn");
        World saved=SaveCodec.decode(SaveCodec.encode(direct));
        for(int turn=0;turn<=6;turn++) {
            byte[] state=SaveCodec.encode(direct);
            check(Arrays.equals(state,SaveCodec.encode(saved)),"direct/load continuation including all logs, reports and RNG at turn "+turn);
            check(direct.turn==turn,"exact turn advancement "+turn);
            check(direct.governance.grades.equals(saved.governance.grades),"stored title state matches visible/saved title "+turn);
            saved=SaveCodec.decode(SaveCodec.encode(saved));
            if(turn<6){direct.nextTurn();saved.nextTurn();}
        }
        check(Arrays.equals(original,legacy),"historical input bytes preserved");
        System.out.println("PASS LegacyGovernanceSaveTest checks="+checks);
    }
}
