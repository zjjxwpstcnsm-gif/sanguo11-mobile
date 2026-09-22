package game.sanguo.core;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** Small data-only JSON codec shared by Android and the repository patch tool.
 * Rejects duplicate keys, fractional/overflow numbers, malformed UTF-8 and excess nesting. */
public final class MapJson {
    public static final int LIMIT=4*1024*1024;
    private MapJson(){}
    public static Object read(InputStream in)throws IOException {
        if(in==null)throw new IOException("无法打开地图文件");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>LIMIT)throw new IOException("地图文件超过4MB");out.write(b,0,n);}
        return parse(out.toByteArray());
    }
    public static Object parse(byte[] data)throws IOException {
        if(data==null||data.length>LIMIT)throw new IOException("地图文件超过4MB");
        try{String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();Parser p=new Parser(text);Object result=p.value(0);p.ws();if(p.i!=text.length())throw p.error("尾部存在额外内容");return result;}
        catch(CharacterCodingException e){throw new IOException("地图文件不是合法UTF-8",e);}
    }
    public static byte[] bytes(Object value){StringBuilder s=new StringBuilder();write(s,value);s.append('\n');return s.toString().getBytes(StandardCharsets.UTF_8);}
    @SuppressWarnings("unchecked") public static Map<String,Object> object(Object value)throws IOException {if(!(value instanceof Map))throw new IOException("需要JSON对象");return (Map<String,Object>)value;}
    @SuppressWarnings("unchecked") public static List<Object> array(Object value)throws IOException {if(!(value instanceof List))throw new IOException("需要JSON数组");return (List<Object>)value;}
    public static int integer(Object value,int lo,int hi)throws IOException {if(!(value instanceof Integer)||((Integer)value)<lo||((Integer)value)>hi)throw new IOException("整数字段越界："+value);return (Integer)value;}
    public static String string(Object value,int max)throws IOException {if(!(value instanceof String)||((String)value).isEmpty()||((String)value).length()>max)throw new IOException("文本字段为空或过长");String s=(String)value;for(int i=0;i<s.length();i++)if(Character.isISOControl(s.charAt(i)))throw new IOException("文本字段包含控制字符");return s;}
    public static boolean bool(Object value)throws IOException {if(!(value instanceof Boolean))throw new IOException("需要布尔字段");return (Boolean)value;}
    public static void keys(Map<String,Object> map,String... names)throws IOException {Set<String> expected=new HashSet<>(Arrays.asList(names));if(!map.keySet().equals(expected))throw new IOException("地图字段缺失或未知："+map.keySet());}
    public static Map<String,Object> obj(Object... pairs){Map<String,Object> map=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)map.put((String)pairs[i],pairs[i+1]);return map;}
    private static void write(StringBuilder s,Object v){
        if(v==null){s.append("null");return;}if(v instanceof String){quote(s,(String)v);return;}
        if(v instanceof Boolean||v instanceof Integer){s.append(v);return;}
        if(v instanceof Map){s.append('{');boolean first=true;for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet()){if(!first)s.append(',');first=false;quote(s,(String)e.getKey());s.append(':');write(s,e.getValue());}s.append('}');return;}
        if(v instanceof Collection){s.append('[');boolean first=true;for(Object x:(Collection<?>)v){if(!first)s.append(',');first=false;write(s,x);}s.append(']');return;}
        throw new IllegalArgumentException("Unsupported JSON value: "+v.getClass());
    }
    private static void quote(StringBuilder s,String text){s.append('"');for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='"'||c=='\\')s.append('\\').append(c);else if(c<' ')s.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else s.append(c);}s.append('"');}
    private static final class Parser {
        final String s;int i,nodes;Parser(String s){this.s=s;}
        IOException error(String message){return new IOException(message+"（字符"+i+"）");}
        void ws(){while(i<s.length()&&(s.charAt(i)==' '||s.charAt(i)=='\n'||s.charAt(i)=='\r'||s.charAt(i)=='\t'))i++;}
        boolean take(char c){ws();if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
        Object value(int depth)throws IOException {
            if(depth>24||++nodes>500000)throw error("JSON结构过深或过大");ws();if(i>=s.length())throw error("JSON意外结束");char c=s.charAt(i);
            if(c=='"')return text();
            if(c=='{'){i++;Map<String,Object> out=new LinkedHashMap<>();if(take('}'))return out;do{ws();if(i>=s.length()||s.charAt(i)!='"')throw error("对象键需要引号");String key=text();if(out.containsKey(key))throw error("JSON重复字段："+key);if(!take(':'))throw error("缺少冒号");out.put(key,value(depth+1));if(take('}'))return out;}while(take(','));throw error("对象分隔符错误");}
            if(c=='['){i++;List<Object> out=new ArrayList<>();if(take(']'))return out;do{out.add(value(depth+1));if(take(']'))return out;}while(take(','));throw error("数组分隔符错误");}
            for(String word:new String[]{"true","false","null"})if(s.startsWith(word,i)){i+=word.length();return word.equals("null")?null:word.equals("true");}
            int start=i;if(c=='-')i++;if(i>=s.length()||!Character.isDigit(s.charAt(i)))throw error("JSON值无效");if(s.charAt(i)=='0')i++;else while(i<s.length()&&s.charAt(i)>='0'&&s.charAt(i)<='9')i++;
            try{return Integer.valueOf(s.substring(start,i));}catch(NumberFormatException e){throw error("只接受32位整数");}
        }
        String text()throws IOException {
            i++;StringBuilder out=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"'){String text=out.toString();for(int j=0;j<text.length();j++){char a=text.charAt(j);if(Character.isHighSurrogate(a)){if(++j>=text.length()||!Character.isLowSurrogate(text.charAt(j)))throw error("UTF-16代理对无效");}else if(Character.isLowSurrogate(a))throw error("UTF-16代理对无效");}return text;}
                if(c<' ')throw error("字符串控制字符无效");if(c=='\\'){if(i>=s.length())throw error("转义未结束");c=s.charAt(i++);switch(c){case '"':case '\\':case '/':break;case 'b':c='\b';break;case 'f':c='\f';break;case 'n':c='\n';break;case 'r':c='\r';break;case 't':c='\t';break;case 'u':if(i+4>s.length())throw error("Unicode转义未结束");try{c=(char)Integer.parseInt(s.substring(i,i+4),16);}catch(NumberFormatException e){throw error("Unicode转义无效");}i+=4;break;default:throw error("未知转义");}}
                out.append(c);if(out.length()>65536)throw error("JSON字符串过长");}throw error("字符串未结束");
        }
    }
}
