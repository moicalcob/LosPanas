package com.gotacar.backend.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import net.minidev.json.JSONObject;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.geo.Point;

import com.gotacar.backend.BackendApplication;
import com.gotacar.backend.models.Location;
import com.gotacar.backend.models.User;
import com.gotacar.backend.models.UserRepository;
import com.gotacar.backend.models.trip.Trip;
import com.gotacar.backend.models.trip.TripRepository;
import com.gotacar.backend.models.tripOrder.TripOrder;
import com.gotacar.backend.models.tripOrder.TripOrderRepository;
import com.gotacar.backend.controllers.TripControllerTest.TestConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = { TestConfig.class, BackendApplication.class })
class TripControllerTest {

	@Configuration
	static class TestConfig {
		@Bean
		@Primary
		public TripRepository mockB() {
			TripRepository mockService = Mockito.mock(TripRepository.class);
			return mockService;
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TripRepository tripRepository;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private TripOrderRepository tripOrderRepository;

	@MockBean
	private RefundController refundController;

	private User user;
	private User admin;
	private User driver;
	private User driver2;
	private Trip trip;
	private TripOrder order;
	private ZonedDateTime actualDate = ZonedDateTime.now();

	@BeforeEach
	void setUp() {
		actualDate = actualDate.withZoneSameInstant(ZoneId.of("Europe/Madrid"));
		List<String> lista1 = new ArrayList<String>();
		lista1.add("ROLE_ADMIN");
		List<String> lista2 = new ArrayList<String>();
		lista2.add("ROLE_CLIENT");
		List<String> lista3 = new ArrayList<String>();
		lista3.add("ROLE_CLIENT");
		lista3.add("ROLE_DRIVER");

		driver = new User("Jesús", "Márquez", "h9HmVQqlBQXD289O8t8q7aN2Gzg1", "driver@gotacar.es", "89070310K",
				"http://dniclient.com", LocalDate.of(1999, 10, 10), lista3, "655757575");
		ObjectId driverObjectId = new ObjectId();
		driver.setId(driverObjectId.toString());

		driver2 = new User("Manuel", "Fernández", "h9HmVQqlBQXD289O8t8q7aN2Gzg2", "driver2@gmail.com", "312312312R",
				"http://dniclient.com", LocalDate.of(1999, 10, 10), lista3, "655757575");
		ObjectId driverObjectId2 = new ObjectId();
		driver2.setId(driverObjectId2.toString());

		user = new User("Martín", "Romero", "qG6h1Pc4DLbPTTTKmXdSxIMEUUE1", "client@gotacar.es", "89070336D",
				"http://dniclient.com", LocalDate.of(1999, 10, 10), lista2, "655757575");
		ObjectId userObjectId = new ObjectId();
		user.setId(userObjectId.toString());

		admin = new User("Antonio", "Fernández", "Ej7NpmWydRWMIg28mIypzsI4BgM2", "admin@gotacar.es", "89070360G",
				"http://dniadmin.com", LocalDate.of(1999, 10, 10), lista1, "655757575");
		ObjectId adminObjectId = new ObjectId();
		admin.setId(adminObjectId.toString());

		Location location1 = new Location("Cerro del Águila", "Calle Canal 48", 37.37536809507917, -5.96211306033204);
		Location location2 = new Location("Viapol", "Av. Diego Martínez Barrio", 37.37625144174958, -5.976345387146261);
		trip = new Trip(location1, location2, 220, LocalDateTime.of(2021, 05, 24, 16, 00, 00),
				LocalDateTime.of(2021, 05, 24, 16, 15, 00), "Viaje desde Cerro del Águila hasta Triana", 3, driver);
		ObjectId tripObjectId = new ObjectId();
		trip.setId(tripObjectId.toString());

		order = new TripOrder(trip, user, LocalDateTime.of(2021, 03, 20, 11, 45, 00), 350, "", 1);
		ObjectId orderObjectId = new ObjectId();
		order.setId(orderObjectId.toString());
	}

	@Test
	void testSearchTrips() throws Exception {
		Point startingPoint = new Point(trip.getStartingPoint().getLng(),trip.getStartingPoint().getLat());
		Point endingPoint = new Point(trip.getEndingPoint().getLng(),trip.getEndingPoint().getLat());
		Mockito.when(tripRepository.searchTrips(startingPoint,endingPoint,trip.getPlaces(),trip.getStartDate())).thenReturn(Arrays.asList(trip));

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("date", "2021-05-24T16:00:00.000+00");
		sampleObject.appendField("places", trip.getPlaces());

		JSONObject startingPointObj = new JSONObject();
		startingPointObj.appendField("name", trip.getStartingPoint().getName());
		startingPointObj.appendField("address", trip.getStartingPoint().getAddress());
		startingPointObj.appendField("lat", trip.getStartingPoint().getLat());
		startingPointObj.appendField("lng", trip.getStartingPoint().getLng());

		sampleObject.appendField("starting_point", startingPointObj);

		JSONObject endingPointObj = new JSONObject();
		endingPointObj.appendField("name", trip.getStartingPoint().getName());
		endingPointObj.appendField("address", trip.getStartingPoint().getAddress());
		endingPointObj.appendField("lat", trip.getEndingPoint().getLat());
		endingPointObj.appendField("lng", trip.getEndingPoint().getLng());

		sampleObject.appendField("ending_point", endingPointObj);

		ResultActions result = mockMvc.perform(post("/search_trips").contentType(MediaType.APPLICATION_JSON)
			.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);

		String res = result.andReturn().getResponse().getContentAsString();
		System.out.println(res);
		int contador = 0;
		while (res.indexOf("startingPoint") > -1) {
			res = res.substring(res.indexOf("startingPoint") + "startingPoint".length(),
			res.length());
			contador++;
		}

		assertThat(contador).isEqualTo(1);
	}

	@Test
	void testFindAllTrips() throws Exception {
		Mockito.when(userRepository.findByUid(admin.getUid())).thenReturn(admin);
		Mockito.when(tripRepository.findAll()).thenReturn(java.util.Arrays.asList(trip));

		String response = mockMvc.perform(post("/user").param("uid", admin.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(get("/list_trips").header("Authorization", token).contentType(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);

		String res = result.andReturn().getResponse().getContentAsString();
		int contador = 0;
		while (res.contains("startingPoint")) {
			res = res.substring(res.indexOf("startingPoint") + "startingPoint".length(), res.length());
			contador++;
		}

		assertThat(contador).isEqualTo(1);
	}
	
	@Test
	void testFindAllTripsWrong() throws Exception {
		Mockito.when(userRepository.findByUid(admin.getUid())).thenReturn(admin);
		Mockito.when(tripRepository.findAll()).thenThrow(new RuntimeException());

		String response = mockMvc.perform(post("/user").param("uid", admin.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(get("/list_trips").header("Authorization", token).contentType(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(404);
	}

	// POSITIVO: Creación de un viaje correctamente
	@Test
	void testCreateTrip() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2031-06-04T13:30:00.000+00");
		sampleObject.appendField("end_date", "2031-06-04T13:50:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);
		assertThat(tripRepository.findAll().size()).isEqualTo(1);
		assertThat(trip.getDriver().getId()).isEqualTo(driver.getId());
	}

	// NEGATIVO Crear usuario con fecha de baneo futura
	@Test
	void testCreateTripBannedUser() throws Exception {
		ZonedDateTime bannedUntil = ZonedDateTime.parse("2022-03-31T13:30:00.000+00");
		bannedUntil = bannedUntil.withZoneSameInstant(ZoneId.of("Europe/Madrid"));
		driver.setBannedUntil(bannedUntil.toLocalDateTime());
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2021-06-04T13:30:00.000+00");
		sampleObject.appendField("end_date", "2021-06-04T13:50:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
		assertThat(result.andReturn().getResponse().getErrorMessage())
				.contains("El usuario esta baneado, no puede realizar esta accion");

	}

	// POSITIVO Crear usuario con fecha de baneo pasada
	@Test
	void testCreateTripNotBannedUser() throws Exception {
		ZonedDateTime bannedUntil = ZonedDateTime.parse("2020-03-31T13:30:00.000+00");
		bannedUntil = bannedUntil.withZoneSameInstant(ZoneId.of("Europe/Madrid"));
		driver.setBannedUntil(bannedUntil.toLocalDateTime());
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2031-06-04T13:30:00.000+00");
		sampleObject.appendField("end_date", "2031-06-04T13:50:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);

	}

	// NEGATIVO: Crear un viaje con la hora de finalización igual a la de inicio
	@Test
	void testCreateTripSameDate() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2031-06-04T13:30:00.000+00");
		sampleObject.appendField("end_date", "2031-06-04T13:30:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
		assertThat(result.andReturn().getResponse().getErrorMessage())
				.isEqualTo("La hora de salida no puede ser igual a la hora de llegada");
	}

	// NEGATIVO: Crear un viaje con la hora de finalización anterior a la de inicio
	@Test
	void testCreateTripEndBeforeStart() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2031-06-04T13:30:00.000+00");
		sampleObject.appendField("end_date", "2031-06-04T13:20:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
		assertThat(result.andReturn().getResponse().getErrorMessage())
				.isEqualTo("La hora de llegada no puede ser anterior a la hora de salida");
	}

	@Test
	void testCreateTripEndClosedStart() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");

		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", "2031-06-04T13:20:00.000+00");
		sampleObject.appendField("end_date", "2031-06-04T13:24:00.000+00");
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
		assertThat(result.andReturn().getResponse().getErrorMessage())
				.isEqualTo("La hora de salida no puede ser tan cercana a la hora de llegada");
	}

	@Test
	void testCreateTripMinStart() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findAll()).thenReturn(Arrays.asList(trip));

		JSONObject starting_poinJsonObject = new JSONObject();
		starting_poinJsonObject.appendField("lat", 37.355465467940405);
		starting_poinJsonObject.appendField("lng", -5.982498103652494);
		starting_poinJsonObject.appendField("name", "Heliopolis");
		starting_poinJsonObject.appendField("address", "Calle Ifni, 41012 Sevilla");
		JSONObject ending_poinJsonObject = new JSONObject();
		ending_poinJsonObject.appendField("lat", 37.355465467940405);
		ending_poinJsonObject.appendField("lng", -5.982498103652494);
		ending_poinJsonObject.appendField("name", "Reina Mercedes");
		ending_poinJsonObject.appendField("address", "Calle Teba, 41012 Sevilla");

		ZonedDateTime dateStartZone = ZonedDateTime.now();
		dateStartZone = dateStartZone.withZoneSameInstant(ZoneId.of("Europe/Madrid"));

		ZonedDateTime dateEndZone = ZonedDateTime.now().plusMinutes(25);
		dateEndZone = dateEndZone.withZoneSameInstant(ZoneId.of("Europe/Madrid"));

		JSONObject sampleObject = new JSONObject();
		sampleObject.appendField("start_date", dateStartZone.toString());
		sampleObject.appendField("end_date", dateEndZone.toString());
		sampleObject.appendField("places", 2);
		sampleObject.appendField("price", 220);
		sampleObject.appendField("comments", "Viaje para el test");
		sampleObject.appendField("starting_point", starting_poinJsonObject);
		sampleObject.appendField("ending_point", ending_poinJsonObject);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc
				.perform(post("/create_trip").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
						.content(sampleObject.toJSONString()).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(400);
		assertThat(result.andReturn().getResponse().getErrorMessage())
				.isEqualTo("El viaje debe ser publicado, al menos, con una hora de antelación");
	}

	@Test
	void testFindTripsByDriver() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findByDriver(driver)).thenReturn(java.util.Arrays.asList(trip));

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc.perform(
				get("/list_trips_driver").header("Authorization", token).contentType(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);

		String res = result.andReturn().getResponse().getContentAsString();
		int contador = 0;
		while (res.contains("startingPoint")) {
			res = res.substring(res.indexOf("startingPoint") + "startingPoint".length(), res.length());
			contador++;
		}

		assertThat(contador).isEqualTo(1);
	}
	@Test
	void testFindTripsByDriverWrong() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findByDriver(driver)).thenThrow(new RuntimeException());

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		ResultActions result = mockMvc.perform(
				get("/list_trips_driver").header("Authorization", token).contentType(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(404);

	}

	@Test
	void testCancelTripDriver() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findById(new
		ObjectId(trip.getId()))).thenReturn(trip);
		Mockito.when(tripOrderRepository.findByTrip(trip)).thenReturn(Arrays.asList(order));

		String response = mockMvc.perform(post("/user").param("uid",
		driver.getUid())).andReturn().getResponse()
		.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		String tripId = trip.getId();

		ResultActions result = mockMvc
		.perform(post("/cancel_trip_driver/{trip_id}",
		tripId).header("Authorization", token)
		.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);
		assertThat(result.andReturn().getResponse().getErrorMessage()).isNull();

	}

	@Test
	void testCancelTripDriverRepeated() throws Exception {
		trip.setCanceled(true);
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripRepository.findById(new ObjectId(trip.getId()))).thenReturn(trip);
		Mockito.when(tripOrderRepository.findByTrip(trip)).thenReturn(Arrays.asList(order));

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		String tripId = trip.getId();

		ResultActions result = mockMvc
				.perform(post("/cancel_trip_driver/{trip_id}", tripId).header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getErrorMessage()).isEqualTo("El viaje ya está cancelado");
	}

	@Test
	void testCancelTripDriverWrongUser() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver2);
		Mockito.when(tripRepository.findById(new ObjectId(trip.getId()))).thenReturn(trip);
		Mockito.when(tripOrderRepository.findByTrip(trip)).thenReturn(Arrays.asList(order));

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		String tripId = trip.getId();

		ResultActions result = mockMvc
				.perform(post("/cancel_trip_driver/{trip_id}", tripId).header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getErrorMessage()).isEqualTo("Usted no ha realizado este viaje");
	}

	@Test
	void testCancelUserFromTrip() throws Exception {
		Mockito.when(userRepository.findByUid(driver.getUid())).thenReturn(driver);
		Mockito.when(userRepository.findByEmail(driver.getEmail())).thenReturn(driver);
		Mockito.when(tripOrderRepository.findById(new ObjectId(order.getId()))).thenReturn(order);

		String response = mockMvc.perform(post("/user").param("uid", driver.getUid())).andReturn().getResponse()
				.getContentAsString();

		org.json.JSONObject json = new org.json.JSONObject(response);
		String token = json.getString("token");

		int beforePlaces = trip.getPlaces();

		ResultActions result = mockMvc
				.perform(post("/trip-order/{trip_order_id}/cancel", order.getId()).header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON));

		assertThat(result.andReturn().getResponse().getStatus()).isEqualTo(200);
		assertThat(result.andReturn().getResponse().getErrorMessage()).isNull();
		assertThat(trip.getPlaces()).isEqualTo(beforePlaces + order.getPlaces());
	}
}
