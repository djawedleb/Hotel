package hotel.tableModel;

import java.util.logging.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import hotel.databaseOperation.DatabaseOperation;
import hotel.databaseOperation.RoomDb;

/**
 *
 * @author Faysal Ahmed
 */

public class BookingTableModel extends AbstractTableModel {
    private static final Logger LOGGER = Logger.getLogger(BookingTableModel.class.getName());
   

    private String[] columnNames;
    
    private transient Object[][] data;

    public BookingTableModel(long start ,long end) {
        iniColNames();
        fetchDataFromDB(start, end);
        
       
    }

    public void iniColNames() {
        Date date;
        date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("d");
        // -1 , because date starts with 0
        int today = ( Integer.parseInt(ft.format(date))-1 )%getMonthLimit(date);
        columnNames = new String[11];
        columnNames[0] = "#";
        for(int i =1;i<11;i++)
        {
            
            today = today%getMonthLimit(date);
            today ++;
            columnNames[i] = today+"";
        }
    }

    public int getMonthLimit(Date x)
    {
        SimpleDateFormat ft = new SimpleDateFormat("M");
        int y = Integer.parseInt(ft.format(x));
        if(y ==2)
            return 28;
        else if (y ==1|| y ==3|| y ==5|| y ==7|| y ==8|| y ==10 || y== 12)
            return 31;
        else return 30;
    }
    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void fetchDataFromDB(long start, long end) {
        try {
            int rows = new RoomDb().getNoOfRooms();
            data = new Object[rows][11];
            initializeDataArray(rows);
            ResultSet roomNames = new RoomDb().getAllRoomNames();
            for (int i = 0; i < rows; i++) {
                if (roomNames.next()) {
                    String roomName = roomNames.getString("room_no");
                    data[i][0] = roomName;
                    processRoomBookings(i, roomName, start, end);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "from Booking table model class\n " + ex.toString());
        }
    }

    private void initializeDataArray(int rows) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < 11; j++) {
                data[i][j] = "";
            }
        }
    }

    private void processRoomBookings(int rowIdx, String roomName, long start, long end) throws SQLException {
        try (DatabaseOperation dbOp = new DatabaseOperation()) {
            ResultSet result = dbOp.getBookingInfo(start, end, roomName);
            while (result.next()) {
            LOGGER.info("coming here for " + roomName);
            long checkIn = parseLong(result.getString("check_in"));
            long checkOut = parseLong(result.getString("check_out"));
            if (checkIn > 0 && checkOut > 0) {
                LOGGER.info(String.format("check in %s .... check out %s", new Date(checkIn * 1000), new Date(checkOut * 1000)));
            }
            if (isFirstLoop(checkIn, checkOut, start, end)) {
                handleFirstLoop(rowIdx, roomName);
            } else if (isWithinRange(checkIn, checkOut, start, end)) {
                handleWithinRange(rowIdx, checkIn, checkOut, start);
            } else if (isLastLoop(checkIn, checkOut, end)) {
                handleLastLoop(rowIdx, checkIn, start);
            }
        }
    }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isFirstLoop(long checkIn, long checkOut, long start, long end) {
        return checkIn <= start && (checkOut == 0 || checkOut <= end);
    }

    private boolean isWithinRange(long checkIn, long checkOut, long start, long end) {
        return checkIn > start && checkOut < end;
    }

    private boolean isLastLoop(long checkIn, long checkOut, long end) {
        return checkIn <= end && (checkOut == 0 || checkOut > end);
    }

    private void handleFirstLoop(int rowIdx, String roomName) {
        LOGGER.info("first LOOP " + roomName);
        data[rowIdx][1] = "<<";
    }

    private void handleWithinRange(int rowIdx, long checkIn, long checkOut, long start) {
        int checkInDay = getDayOfMonth(checkIn);
        int checkOutDay = getDayOfMonth(checkOut);
        int today = getDayOfMonth(start);
        LOGGER.info("xxxxxxxxx " + today + "........ " + checkInDay);
        data[rowIdx][(checkInDay - today) + 1] = ">";
        data[rowIdx][(checkOutDay - today) + 1] = "<";
    }

    private void handleLastLoop(int rowIdx, long checkIn, long start) {
        int checkInDay = getDayOfMonth(checkIn);
        int today = getDayOfMonth(start);
        LOGGER.info("..... " + today + " ...........  " + checkInDay);
        data[rowIdx][(checkInDay - today) + 1] = ">>";
    }

    private int getDayOfMonth(long timestamp) {
        return Integer.parseInt(new SimpleDateFormat("d").format(new Date(timestamp * 1000)));
    }
}
