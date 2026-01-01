package org.mirage.ccio.app.famsApi;

import dan200.computercraft.api.lua.LuaException;
import org.mirage.fams.central.FamsTypes;

import java.util.List;
import java.util.Map;

/**
 * Strict parsing helpers for Lua -> Java values.
 */
public final class LuaParsing {
    private LuaParsing(){}

    public static String getString(Map<String,Object> map, String key, String def){
        Object v = map.get(key);
        if(v == null) return def;
        if(v instanceof String s) return s;
        return String.valueOf(v);
    }

    public static int getInt(Map<String,Object> map, String key, int def, int min, int max) throws LuaException{
        Object v = map.get(key);
        if(v == null) return def;
        if(v instanceof Number n){
            int i = n.intValue();
            if(i < min || i > max) throw new LuaException("config." + key + " out of range: " + i);
            return i;
        }
        throw new LuaException("config." + key + " must be a number");
    }

    public static Integer getNullableInt(Map<String,Object> map, String key, Integer def, int min, int max) throws LuaException{
        Object v = map.get(key);
        if(v == null) return def;
        if(v instanceof Number n){
            int i = n.intValue();
            if(i < min || i > max) throw new LuaException("config." + key + " out of range: " + i);
            return i;
        }
        throw new LuaException("config." + key + " must be a number");
    }

    public static double[] toDoubleArray(Object value) throws LuaException{
        if(value == null) return new double[0];
        if(value instanceof Number n){
            return new double[]{n.doubleValue()};
        }
        if(value instanceof List<?> list){
            double[] out = new double[list.size()];
            for(int i=0;i<list.size();i++){
                Object e = list.get(i);
                if(!(e instanceof Number nn)) throw new LuaException("Array element must be a number at index " + (i+1));
                out[i] = nn.doubleValue();
            }
            return out;
        }
        if(value instanceof Object[] arr){
            double[] out = new double[arr.length];
            for(int i=0;i<arr.length;i++){
                Object e = arr[i];
                if(!(e instanceof Number nn)) throw new LuaException("Array element must be a number at index " + (i+1));
                out[i] = nn.doubleValue();
            }
            return out;
        }
        throw new LuaException("Expected number or array of numbers");
    }

    public static FamsTypes.SystemMode parseSystemMode(String modeName) throws LuaException{
        if(modeName == null) throw new LuaException("mode is null");
        String m = modeName.trim().toUpperCase();
        try{
            return FamsTypes.SystemMode.valueOf(m);
        }catch(IllegalArgumentException e){
            throw new LuaException("Unknown mode: " + modeName + ". Valid: SLEEP, PARTIAL_AUTO, FORMAL, EMERGENCY");
        }
    }
}
