using System;
namespace Sanguo.Contracts
{
    // Dimensionless presentation coordinates, NOT path cost or attack rules.
    // Same fixtures as Java GridLayout; source-image origins are metadata, not an extra world offset.
    public sealed class GridLayout
    {
        public readonly bool ColumnStaggered;
        public readonly double Offset;
        public readonly int SourceOriginX, SourceOriginY;
        public GridLayout(bool columnStaggered,double offset,int originX,int originY)
        { ColumnStaggered=columnStaggered;Offset=offset;SourceOriginX=originX;SourceOriginY=originY; }
        public double X(int q,int r) { return ColumnStaggered?r:q+r*.5-Offset; }
        public double Z(int q,int r) { return ColumnStaggered?q+r*.5-Offset:r; }
        public int Row(double x,double z) { return (int)Math.Floor((ColumnStaggered?x:z)+.5); }
        public int Column(double x,double z) { return (int)Math.Floor((ColumnStaggered?z:x)-Row(x,z)*.5+Offset+.5); }
    }
}
