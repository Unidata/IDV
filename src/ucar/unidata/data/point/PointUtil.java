package ucar.unidata.data.point;

import java.util.Arrays;
import java.util.Stack;

public class PointUtil {
     /*************************************************************************
      *  Compilation:  javac GrahamaScan.java
      *  Execution:    java GrahamScan < input.txt
      *  Dependencies: Point2D.java
      *
      *  Create points from standard input and compute the convex hull using
      *  Graham scan algorithm.
      *
      *  May be floating-point issues if x- and y-coordinates are not integers.
      *
      *************************************************************************/

    public class GrahamScan {
        private Stack<LatLonPoint2D> hull = new Stack<LatLonPoint2D>();

            public GrahamScan(LatLonPoint2D[] pts) {

                  // defensive copy
                int N = pts.length;
                LatLonPoint2D[] points = new LatLonPoint2D[N];
                for (int i = 0; i < N; i++)
                        points[i] = pts[i];

                 // preprocess so that points[0] has lowest y-coordinate; break ties by x-coordinate
                 // points[0] is an extreme point of the convex hull
                 // (alternatively, could do easily in linear time)
                 Arrays.sort(points);

                 // sort by polar angle with respect to base point points[0],
                 // breaking ties by distance to points[0]
                 Arrays.sort(points, 1, N, points[0].R_ORDER);

                 hull.push(points[0]);       // p[0] is first extreme point

                 // find index k1 of first point not equal to points[0]
                 int k1;
                 for (k1 = 1; k1 < N; k1++)
                         if (!points[0].equals(points[k1])) break;
                 if (k1 == N) return;        // all points equal

                 // find index k2 of first point not collinear with points[0] and points[k1]
                 int k2;
                  for (k2 = k1 + 1; k2 < N; k2++)
                         if (LatLonPoint2D.ccw(points[0], points[k1], points[k2]) != 0) break;
                 hull.push(points[k2-1]);    // points[k2-1] is second extreme point

                // Graham scan; note that points[N-1] is extreme point different from points[0]
                 for (int i = k2; i < N; i++) {
                     LatLonPoint2D top = hull.pop();
                         while (LatLonPoint2D.ccw(hull.peek(), top, points[i]) <= 0) {
                                 top = hull.pop();
                             }
                         hull.push(top);
                         hull.push(points[i]);
                     }

                 assert isConvex();
                 }

            // return extreme points on convex hull in counterclockwise order as an Iterable
              public Iterable<LatLonPoint2D> hull() {
                     Stack<LatLonPoint2D> s = new Stack<LatLonPoint2D>();
                     for (LatLonPoint2D p : hull) s.push(p);
                    return s;
                }

            // check that boundary of hull is strictly convex
              private boolean isConvex() {
                     int N = hull.size();
                     if (N <= 2) return true;

                  LatLonPoint2D[] points = new LatLonPoint2D[N];
                     int n = 0;
                     for (LatLonPoint2D p : hull()) {
                            points[n++] = p;
                         }

                    for (int i = 0; i < N; i++) {
                            if (LatLonPoint2D.ccw(points[i], points[(i+1) % N], points[(i+2) % N]) <= 0) {
                                     return false;
                                 }
                        }
                    return true;
                }

              // test client
              public   void main(String[] args) {

                }

         }
}
