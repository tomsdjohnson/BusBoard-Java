package training.busboard.web;

//everything we need to import//
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import training.busboard.Bus;
import training.busboard.BusStopID;
import training.busboard.ClosestBuses;
import training.busboard.Coordinates;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@EnableAutoConfiguration
public class Website {

//    //Creates a list for about us info
//    List<AboutUsInfo> aboutUsInfoList = new ArrayList<>();
//
//    //Reads this file
//    String filename = "C:\\Users\\JJG\\Work\\BusBoard1\\BusBoardAboutUsTable.csv";
//    List<String> lines = readTheFile(filename);
//
//
//    aboutus = CreateInfo(lines);


    // when it is just "/" it tells it to go to index.html//
    @RequestMapping("/")
    ModelAndView home() {
        HomeModel homeModel = new HomeModel();
        return new ModelAndView("index", "homeModel", homeModel);
    }

    @RequestMapping("/aboutus")
    ModelAndView aboutUs() {
        AboutUs aboutUs = new AboutUs();
        return new ModelAndView("aboutus", "AboutUs", aboutUs);
    }

    //requests postcode as string//
    @RequestMapping("/busInfo")
    ModelAndView busInfo(@RequestParam("postcode") String postcode) {

        //creates builder for json reader//
        Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();

        Coordinates postcodeInfo = null;

        //creates Coordinates and CoordinatesResult object, gets Long and Lat//
        try {
            postcodeInfo = client
                    .target("https://api.postcodes.io/postcodes/" + postcode)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(new GenericType<Coordinates>() {
                    });

        } catch (Exception e) {
            HomeModel homeModel = new HomeModel();
            homeModel.error = "Invalid Postcode!";
            return new ModelAndView("index", "homeModel", homeModel);
        }


        //gets the busStop id from long and lat input as an Object "BusStopID"//
        BusStopID busID = client
                .target("https://api-argon.tfl.gov.uk/StopPoint?stopTypes=NaptanPublicBusCoachTram&radius=1000&lat="
                        + postcodeInfo.result.latitude + "&lon=" + postcodeInfo.result.longitude)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<BusStopID>() {
                });

        //creates an array for busStop object//
        ArrayList<BusStop> busStops = new ArrayList<>();


        //for loop that iterates through the BusStops and puts them in the Array//
        for (int i = 0; i < 10; i++) {
            //creates new object BusStop//
            BusStop stop = new BusStop();

            //gets the name and the id of the busStop from busID//
            String name = busID.stopPoints.get(i).commonName;
            String id = busID.stopPoints.get(i).stopLetter;

            //creates string and makes it equal to commonName within object stop//
            if(id == null){
                stop.commonName = name;
            }else {
                stop.commonName = name + " (Stop " + id + ")";
            }

            //creates an array full of object Bus that are coming to that stop//
            ArrayList<Bus> busList = client
                    .target("https://api-argon.tfl.gov.uk/StopPoint/" + busID.stopPoints.get(i).id + "/Arrivals")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(new GenericType<ArrayList<Bus>>() {
                    });
            //puts the array into class closest BusList to order them and get the next five buses//
            ArrayList<Bus> closestBusList = ClosestBuses.getCloseBuses(busList);
            //makes the array full of object bus equal to BusStop buses//
            stop.buses = closestBusList;

            //adds the object BusStop to stop Array//
            busStops.add(stop);
        }

        // Make Model object and make busStop equal to array busStop//
        Model model = new Model();
        model.busStops = busStops;

        // Tells where your sending in ("info")//
        // Class you can access (model) and how to reference it in the html ("model")
        return new ModelAndView("info", "model", model);
    }

    //entry point to the app//
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Website.class, args);
    }

    public static List<String> readTheFile(String filename) {
        Path path = Paths.get(filename);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

//    public static List<AboutUsInfo> CreateInfo(List<String> lines) {
//        ArrayList<AboutUsInfo> as = new ArrayList<AboutUsInfo>();
//
//        for (int i = 1; i < lines.size(); i++) {
//            String line = lines.get(i);
//
//            String[] bits = line.split(",");
//
//            AboutUsInfo a = new AboutUsInfo();
//            a.FirstName = bits[0];
//            a.LastName = bits[1];
//            a.DateOfBirth = LocalDate.parse(bits[2], DateTimeFormatter.ofPattern("dd/MM/yyyy"));
//            a.FavFilm = bits[3];
//            a.FavFood = bits[4];
//            as.add(a);
//
//        }
//
//        return as;
//    }
}