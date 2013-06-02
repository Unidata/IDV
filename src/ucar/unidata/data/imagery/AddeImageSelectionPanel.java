package ucar.unidata.data.imagery;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.rmi.RemoteException;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidas.AreaFile;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.geoloc.*;
import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.visad.MapProjectionProjection;
import ucar.visad.display.RubberBandBox;
import visad.*;
import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.NavigatedCoordinateSystem;

/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/28/13
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageSelectionPanel extends JPanel implements MouseListener, MouseMotionListener {
    MapProjection proj;
    GeneralPath gp;
    private static final int CONTROL_POINT_SIZE = 10;
    java.awt.geom.Rectangle2D.Float boundingBox;
    private AddeImageDescriptor preview_image_descriptor;
    private BufferedImage preview_image;
    private boolean keep_drawing;
    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private boolean first_time;
    private java.awt.geom.Rectangle2D.Float ul_cp;
    private java.awt.geom.Rectangle2D.Float ur_cp;
    private java.awt.geom.Rectangle2D.Float ll_cp;
    private java.awt.geom.Rectangle2D.Float lr_cp;
    private int curr_cp_indx;
    private boolean created;
    int threshold_data_width;
    int threshold_data_height;
    private int track_point_x;
    private int track_point_y;
    private boolean move_rect;
    private int data_width;
    private int data_height;
    private PropertyChangeSupport pcs;
    NavigatedMapPanel mapPanel;

    public void clearBoundingBox()
    {
        startX = -1;
        startY = -1;
        endX = -1;
        endY = -1;
        curr_cp_indx = -1;
        first_time = true;
        created = false;
        keep_drawing = false;
        ul_cp = null;
        ur_cp = null;
        ll_cp = null;
        lr_cp = null;
        if(null != boundingBox)
        {
            boundingBox = null;
            repaint();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl)
    {
        pcs.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl)
    {
        pcs.removePropertyChangeListener(pcl);
    }

    public AddeImageSelectionPanel(BufferedImage preview_image,  AddeImageDescriptor preview_image_descriptor)
    {
        boundingBox = null;
        keep_drawing = false;
        startX = -1;
        startY = -1;
        endX = -1;
        endY = -1;
        first_time = true;
        curr_cp_indx = -1;
        created = false;
        move_rect = false;
        this.preview_image_descriptor = preview_image_descriptor;
        this.preview_image = preview_image;
        try{
            AreaFile af = new AreaFile(preview_image_descriptor.getSource());
            AREACoordinateSystem acs = null;
            acs = new AREACoordinateSystem(af);
            this.proj = acs;
        } catch (Exception e){}

        data_width = preview_image.getWidth();
        data_height = preview_image.getHeight();
        threshold_data_height = data_height;
        threshold_data_width = data_width;

        gp = new GeneralPath();
        readBaseMap();
        addMouseListener(this);
        addMouseMotionListener(this);
        setToolTipText("Use mouse drag and resize the bounding box");
        pcs = new PropertyChangeSupport(this);

        init();

    }

    private void init()
    {
        Dimension d = new Dimension(data_width, data_height);
        setPreferredSize(d);
        setSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        int initial_window_width = Math.min(data_width, threshold_data_width);
        int initial_window_height = Math.min(data_height, threshold_data_height);
        startX = 0;
        startY = 0;
        endX = initial_window_width;
        endY = initial_window_height;
        ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
        ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
        ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
        lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
        boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
        pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
        first_time = false;
        created = true;
        move_rect = false;
    }

    public void resetBoundingBox()
    {
        int initial_window_width = Math.min(data_width, threshold_data_width);
        int initial_window_height = Math.min(data_height, threshold_data_height);
        startX = 0;
        startY = 0;
        endX = initial_window_width;
        endY = initial_window_height;
        ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
        ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
        ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
        lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
        boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
        pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
        first_time = false;
        created = true;
        move_rect = false;
        repaint();
    }

    public void updateImage(BufferedImage preview_image)
    {
        this.preview_image = preview_image;
        data_width = preview_image.getWidth();
        data_height = preview_image.getHeight();
        threshold_data_height = data_height;
        threshold_data_width = data_width;
        init();
        repaint();
    }

    public java.awt.geom.Rectangle2D.Float getBoundingBox()
    {
        return boundingBox;
    }

    public void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        if(null != preview_image)
            g2d.drawImage(preview_image, 0, 0, this);
        g2d.setColor(Color.BLUE);
        g2d.draw(gp);
        g2d.setColor(Color.CYAN);
        if(created)
        {
            g2d.drawRect(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
            g2d.setColor(Color.BLUE);
            g2d.fillRect(startX, startY, 10, 10);
            g2d.fillRect(endX, startY, 10, 10);
            g2d.fillRect(startX, endY, 10, 10);
            g2d.fillRect(endX, endY, 10, 10);
        }
    }

    private void readBaseMap()
    {
        SampledSet sets[] = null;
        BaseMapAdapter bma = null;
        try
        {
            bma = new BaseMapAdapter(getClass().getResource("/auxdata/maps/OUTLSUPW"));
            sets = bma.getData().getSets();
        }
        catch(IOException ex) { }
        catch(VisADException ex) { }
        float samples[][] = (float[][])null;
        ProjectionRect rect = new ProjectionRect(proj.getDefaultMapArea());
        ProjectionPoint pp_ul = rect.getUpperLeftPoint();
        ProjectionPoint pp_lr = rect.getLowerRightPoint();
        double top_left_x = pp_ul.getX();
        double top_left_y = pp_ul.getY();
        double proj_rect_width = pp_lr.getX() - top_left_x;
        double proj_rect_height = pp_lr.getY() - top_left_y;
        boolean flip_y = false;
        double step_x = (double)data_width / proj_rect_width;
        double step_y = (double)data_height / proj_rect_height;
        for(int i = 0; i < sets.length; i++)
        {
            try
            {
                samples = sets[i].getSamples(false);
            }
            catch(VisADException ex)
            {
                ex.printStackTrace();
            }
            java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
            boolean flag = false;
            MapProjectionProjection mpp = new MapProjectionProjection(proj);
            for(int j = 0; j < samples[0].length; j++)
            {
                ProjectionPointImpl ppi = mpp.latLonToProj(samples[0][j], samples[1][j]);
                double x = ppi.getX();
                double y = ppi.getY();
                if(java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y))
                    continue;
                x = (x - top_left_x) * step_x;
                y = (y - top_left_y) * step_y;
                if(flip_y)
                    y = (double)data_height - y - 1.0D;
                if(x < 0.0D || x >= (double)data_width || y < 0.0D || y >= (double)data_height)
                    continue;
                if(!flag)
                {
                    p.moveTo(x, y);
                    flag = true;
                } else
                {
                    p.lineTo(x, y);
                }
            }

            gp.append(p, false);
            samples = (float[][])null;
        }

        sets = null;
        bma = null;
    }

    public void mouseClicked(MouseEvent me)
    {
        if(first_time)
        {
            keep_drawing = true;
            startX = me.getX();
            startY = me.getY();
        } else
        {
            int pt_x = me.getX();
            int pt_y = me.getY();
            if(ul_cp == null)
                ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
            if(ur_cp == null)
                ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
            if(ll_cp == null)
                ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
            if(lr_cp == null)
                lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
            if(ul_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 0;
                setCursor(new Cursor(12));
            } else
            if(ur_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 1;
                setCursor(new Cursor(12));
            } else
            if(ll_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 2;
                setCursor(new Cursor(12));
            } else
            if(lr_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 3;
                setCursor(new Cursor(12));
            } else
            if(boundingBox.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(13));
                track_point_x = pt_x;
                track_point_y = pt_y;
                move_rect = true;
            }
        }
        requestFocus(true);
    }

    public void mousePressed(MouseEvent me)
    {
        if(first_time)
        {
            keep_drawing = true;
            startX = me.getX();
            startY = me.getY();
        } else
        {
            int pt_x = me.getX();
            int pt_y = me.getY();
            if(ul_cp == null)
                ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
            if(ur_cp == null)
                ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
            if(ll_cp == null)
                ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
            if(lr_cp == null)
                lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
            if(ul_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 0;
                setCursor(new Cursor(12));
            } else
            if(ur_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 1;
                setCursor(new Cursor(12));
            } else
            if(ll_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 2;
                setCursor(new Cursor(12));
            } else
            if(lr_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                curr_cp_indx = 3;
                setCursor(new Cursor(12));
            } else
            if(boundingBox.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(13));
                track_point_x = pt_x;
                track_point_y = pt_y;
                move_rect = true;
            }
        }
        requestFocus(true);
    }

    public void mouseReleased(MouseEvent me)
    {
        int pt_x = me.getX();
        int pt_y = me.getY();
        boolean inside_data_window = pt_x >= 0 && pt_x < data_width && pt_y >= 0 && pt_y < data_height;
        if(inside_data_window)
            if(first_time)
            {
                first_time = false;
                if(shouldRedraw_firstTime(pt_x, pt_y))
                {
                    boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
                    pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
                    repaint();
                }
            } else if(move_rect)
            {
                int trans_x = pt_x - track_point_x;
                int trans_y = pt_y - track_point_y;
                if(shouldTranslate(trans_x, trans_y))
                    translate_rect(trans_x, trans_y);
                track_point_x = -1;
                track_point_y = -1;
                move_rect = false;
            } else if(shouldRedraw(me))
            {
                updateSelectedCP(me);
                repaint();
            }
        setCursor(Cursor.getDefaultCursor());
        keep_drawing = false;
        curr_cp_indx = -1;
    }

    private void updateSelectedCP(MouseEvent me)
    {
        if(curr_cp_indx == 0)
        {
            startX = me.getX();
            startY = me.getY();
        }
        if(curr_cp_indx == 1)
        {
            endX = me.getX();
            startY = me.getY();
        }
        if(curr_cp_indx == 2)
        {
            startX = me.getX();
            endY = me.getY();
        }
        if(curr_cp_indx == 3)
        {
            endX = me.getX();
            endY = me.getY();
        }
        ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
        ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
        ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
        lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
        boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
        pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
    }

    public void mouseEntered(MouseEvent mouseevent)
    {
    }

    public void mouseExited(MouseEvent mouseevent)
    {
    }

    public void mouseDragged(MouseEvent me)
    {
        int pt_x = me.getX();
        int pt_y = me.getY();
        if(ul_cp.contains(pt_x, pt_y))
        {
            keep_drawing = true;
            curr_cp_indx = 0;
            setCursor(new Cursor(12));
        } else if(ur_cp.contains(pt_x, pt_y))
        {
            keep_drawing = true;
            curr_cp_indx = 1;
            setCursor(new Cursor(12));
        } else if(ll_cp.contains(pt_x, pt_y))
        {
            keep_drawing = true;
            curr_cp_indx = 2;
            setCursor(new Cursor(12));
        } else if(lr_cp.contains(pt_x, pt_y))
        {
            keep_drawing = true;
            curr_cp_indx = 3;
            setCursor(new Cursor(12));
        } else if(boundingBox.contains(pt_x, pt_y))
        {
            keep_drawing = true;
            setCursor(new Cursor(13));
            //track_point_x = pt_x;
            //track_point_y = pt_y;
            move_rect = true;
        }
        boolean inside_data_window = true; //pt_x >= 0 && pt_x < data_width && pt_y >= 0 && pt_y < data_height;
        if(inside_data_window && keep_drawing)
        {
            if(first_time)
            {
                if(shouldRedraw_firstTime(pt_x, pt_y))
                {
                    endX = pt_x;
                    endY = pt_y;
                    created = true;
                    ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
                    ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
                    ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
                    lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
                    boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
                    pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
                }
            } else if(move_rect) {
                int trans_x = pt_x - track_point_x;
                int trans_y = pt_y - track_point_y;
                if(shouldTranslate(trans_x, trans_y))
                    translate_rect(trans_x, trans_y);
                track_point_x = pt_x;
                track_point_y = pt_y;
                move_rect = false;
            } else if(shouldRedraw(me)) {
                updateSelectedCP(me);
            }

            repaint();
        }
    }

    public void mouseMoved(MouseEvent me)
    {
        if(boundingBox != null)
        {
            int pt_x = me.getX();
            int pt_y = me.getY();
            if(ul_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(12));
            } else
            if(ur_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(12));
            } else
            if(ll_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(12));
            } else
            if(lr_cp.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(12));
            } else
            if(boundingBox.contains(pt_x, pt_y))
            {
                keep_drawing = true;
                setCursor(new Cursor(13));
            } else
            if(!getCursor().equals(Cursor.getDefaultCursor()))
                setCursor(Cursor.getDefaultCursor());
        }
    }

    private boolean shouldRedraw_firstTime(int pt_x, int pt_y)
    {
        int local_data_width = Math.abs(startX - pt_x);
        int local_data_height = Math.abs(startY - pt_y);
        return local_data_width <= threshold_data_width && local_data_height <= threshold_data_height;
    }

    private boolean shouldRedraw(MouseEvent me)
    {
        int local_data_width = -1;
        int local_data_height = -1;
        int pt_x = me.getX();
        int pt_y = me.getY();
        if(curr_cp_indx == 0)
        {
            local_data_width = Math.abs(endX - me.getX());
            local_data_height = Math.abs(endY - me.getY());
        }
        if(curr_cp_indx == 1)
        {
            local_data_width = Math.abs(pt_x - startX);
            local_data_height = Math.abs(endY - pt_y);
        }
        if(curr_cp_indx == 2)
        {
            local_data_width = Math.abs(endX - pt_x);
            local_data_height = Math.abs(pt_y - startY);
        }
        if(curr_cp_indx == 3)
        {
            local_data_width = Math.abs(pt_x - startX);
            local_data_height = Math.abs(pt_y - startY);
        }
        return local_data_width <= threshold_data_width && local_data_height <= threshold_data_height;
    }

    private boolean shouldTranslate(int transX, int transY)
    {
        int new_startX = startX + transX;
        int new_startY = startY + transY;
        int new_endX = endX + transX;
        int new_endY = endY + transY;
        return new_startX >= 0 && new_startY >= 0 && new_endX < data_width && new_endY < data_height;
    }

    private void translate_rect(int transX, int transY)
    {
        startX += transX;
        startY += transY;
        endX += transX;
        endY += transY;
        ur_cp = new java.awt.geom.Rectangle2D.Float(endX, startY, 10F, 10F);
        ul_cp = new java.awt.geom.Rectangle2D.Float(startX, startY, 10F, 10F);
        ll_cp = new java.awt.geom.Rectangle2D.Float(startX, endY, 10F, 10F);
        lr_cp = new java.awt.geom.Rectangle2D.Float(endX, endY, 10F, 10F);
        boundingBox = new java.awt.geom.Rectangle2D.Float(startX >= endX ? endX : startX, startY >= endY ? endY : startY, Math.abs(endX - startX), Math.abs(endY - startY));
        pcs.firePropertyChange("BOUNDING_BOX_CHANGED", null, boundingBox);
    }





}