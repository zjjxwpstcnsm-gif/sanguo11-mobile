package game.sanguo.core;

import java.util.*;

/** Derived geographical catchments, not original SAN11 border data or movement permissions.
 * Stable site IDs break equal-distance ties. Ownership and diplomacy are read live. */
public final class Territory {
    private static final int[][] DIRECTIONS={{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}};
    private final World world;
    private final int[][] sites;
    private final Map<Integer,World.City> cities=new TreeMap<>();
    private final Map<Integer,Set<Integer>> neighbors=new TreeMap<>();
    private final Map<Integer,Integer> sizes=new TreeMap<>();
    private static final class Step {
        final int q,r,site,cost;
        Step(int q,int r,int site,int cost){this.q=q;this.r=r;this.site=site;this.cost=cost;}
    }
    public Territory(World world){
        this.world=Objects.requireNonNull(world);sites=new int[world.width][world.height];
        int[][] costs=new int[world.width][world.height];
        for(int q=0;q<world.width;q++){Arrays.fill(sites[q],-1);Arrays.fill(costs[q],Integer.MAX_VALUE);}
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt((Step s)->s.cost)
            .thenComparingInt(s->s.site).thenComparingInt(s->s.q).thenComparingInt(s->s.r));
        for(World.City city:world.cities){
            cities.put(city.id,city);neighbors.put(city.id,new TreeSet<>());
            for(Hex h:SiteFootprint.cells(city)){
                sites[h.q][h.r]=city.id;costs[h.q][h.r]=0;
                queue.add(new Step(h.q,h.r,city.id,0));
            }
        }
        while(!queue.isEmpty()){
            Step s=queue.remove();if(costs[s.q][s.r]!=s.cost||sites[s.q][s.r]!=s.site)continue;
            for(int[] d:DIRECTIONS){int q=s.q+d[0],r=s.r+d[1];if(!inside(q,r))continue;
                World.Terrain terrain=world.terrain[q][r];if(terrain==World.Terrain.MOUNTAIN||terrain==World.Terrain.VOID||terrain==World.Terrain.SEA||terrain==World.Terrain.NON_NAVIGABLE_WATER)continue;
                int step=terrain==World.Terrain.PLAIN?2:terrain==World.Terrain.WATER?5:3;
                int cost=s.cost+step;
                if(cost>costs[q][r]||cost==costs[q][r]&&s.site>=sites[q][r])continue;
                costs[q][r]=cost;sites[q][r]=s.site;queue.add(new Step(q,r,s.site,cost));
            }
        }
        for(int q=0;q<world.width;q++)for(int r=0;r<world.height;r++){
            int site=siteAt(q,r);if(site<0)continue;sizes.merge(site,1,Integer::sum);
            for(int[] d:DIRECTIONS){int other=siteAt(q+d[0],r+d[1]);if(other>=0&&other!=site)neighbors.get(site).add(other);}
        }
    }
    private boolean inside(int q,int r){return world.sourceInside(new Hex(q,r));}
    public int siteAt(int q,int r){return inside(q,r)?sites[q][r]:-1;}
    public int siteAt(Hex hex){return hex==null?-1:siteAt(hex.q,hex.r);}
    public int ownerAt(int q,int r){World.City c=cities.get(siteAt(q,r));return c==null?-1:c.owner;}
    public int size(int site){return sizes.getOrDefault(site,0);}
    public Set<Integer> neighbors(int site){return Collections.unmodifiableSet(neighbors.getOrDefault(site,Collections.emptySet()));}
    public boolean frontline(int site){
        World.City city=cities.get(site);if(city==null||city.owner<0)return false;
        for(int id:neighbors(site)){World.City other=cities.get(id);if(other.owner>=0&&world.campaign.hostile(city.owner,other.owner))return true;}
        return false;
    }
    /** The six bits follow Hex.neighbors(): east, northeast, northwest, west, southwest, southeast. */
    public int boundary(int q,int r,boolean perSite){
        int site=siteAt(q,r);if(site<0)return 0;int owner=ownerAt(q,r),mask=0;
        for(int i=0;i<6;i++){int nq=q+DIRECTIONS[i][0],nr=r+DIRECTIONS[i][1],other=siteAt(nq,nr);
            if(other!=site&&(perSite||other<0||ownerAt(nq,nr)!=owner||owner<0))mask|=1<<i;
        }
        return mask;
    }
}
