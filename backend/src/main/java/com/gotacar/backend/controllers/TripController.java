package com.gotacar.backend.controllers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotacar.backend.models.Location;
import com.gotacar.backend.models.User;
import com.gotacar.backend.models.UserRepository;
import com.gotacar.backend.models.trip.Trip;
import com.gotacar.backend.models.trip.TripRepository;
import com.gotacar.backend.models.tripOrder.TripOrder;
import com.gotacar.backend.models.tripOrder.TripOrderRepository;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
public class TripController {
	
	private static final String TIME_ZONE = "Europe/Madrid";

	@Autowired
	private RefundController refundController;

	@Autowired
	private TripOrderRepository tripOrderRepository;

	@Autowired
	private TripRepository tripRepository;

	@Autowired
	private UserRepository userRepository;

	private static ObjectMapper objectMapper = new ObjectMapper();

	@PostMapping(path = "/search_trips")
	public List<Trip> searchTrip(@RequestBody() String body) {
		try {
			var jsonNode = objectMapper.readTree(body);

			Integer placesJson = null;
			var startingPoint = new Point(0, 0);
			var endingPoint = new Point(0, 0);

			try {
				JsonNode startingPointJson = objectMapper.readTree(jsonNode.get("starting_point").toString());
				startingPoint = new Point(startingPointJson.get("lng").asDouble(),
					startingPointJson.get("lat").asDouble());
			} catch (Exception e) {
			}

			try {
				JsonNode endingPointJson = objectMapper.readTree(jsonNode.get("ending_point").toString());
				endingPoint = new Point(endingPointJson.get("lng").asDouble(),
					endingPointJson.get("lat").asDouble());
			} catch (Exception e) {
			}

			try {
				placesJson = objectMapper.readTree(jsonNode.get("places").toString()).asInt();
			} catch (Exception e) {
			}

			var dateJson = OffsetDateTime
					.parse(objectMapper.readTree(jsonNode.get("date").toString()).asText()).toLocalDateTime();
			
			List<Trip> trips = tripRepository.searchTrips(startingPoint, endingPoint, placesJson, dateJson);

			return trips.stream().filter(x -> x.getCanceled().equals(false) || x.getCanceled() == null)
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}

	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@GetMapping("/list_trips")
	public List<Trip> listTrips() {
		try {
			return tripRepository.findAll();
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@PreAuthorize("hasRole('ROLE_DRIVER')")
	@PostMapping("/create_trip")
	public Trip createTrip(@RequestBody() String body) {
		try {
			var actualDate = ZonedDateTime.now();
			actualDate = actualDate.withZoneSameInstant(ZoneId.of(TIME_ZONE));

			var jsonNode = objectMapper.readTree(body);

			JsonNode startingPointJson = objectMapper.readTree(jsonNode.get("starting_point").toString());

			var startingPoint = new Location(startingPointJson.get("name").asText(),
					startingPointJson.get("address").asText(), startingPointJson.get("lat").asDouble(),
					startingPointJson.get("lng").asDouble());

			JsonNode endingPointJson = objectMapper.readTree(jsonNode.get("ending_point").toString());

			var endingPoint = new Location(endingPointJson.get("name").asText(),
					endingPointJson.get("address").asText(), endingPointJson.get("lat").asDouble(),
					endingPointJson.get("lng").asDouble());

			Integer placesJson = objectMapper.readTree(jsonNode.get("places").toString()).asInt();

			Integer price = objectMapper.readTree(jsonNode.get("price").toString()).asInt();

			var dateStartZone = ZonedDateTime
					.parse(objectMapper.readTree(jsonNode.get("start_date").toString()).asText());
			dateStartZone = dateStartZone.withZoneSameInstant(ZoneId.of(TIME_ZONE));

			var dateStartJson = dateStartZone.toLocalDateTime();

			var dateEndZone = ZonedDateTime
					.parse(objectMapper.readTree(jsonNode.get("end_date").toString()).asText());
			dateEndZone = dateEndZone.withZoneSameInstant(ZoneId.of(TIME_ZONE));

			var dateEndJson = dateEndZone.toLocalDateTime();

			String comments = objectMapper.readTree(jsonNode.get("comments").toString()).asText();

			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var currentUser = userRepository.findByEmail(authentication.getPrincipal().toString());
			LocalDateTime bannedUntil = currentUser.getBannedUntil();
			if (bannedUntil != null) {
				if (bannedUntil.isAfter(actualDate.toLocalDateTime())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"El usuario esta baneado, no puede realizar esta accion");
				}
			}

			LocalDateTime cancelationDateLimit = dateStartJson.minusMinutes(30);

			// Lanza error si la fecha de finalizacion es anterior a la de salida
			if (dateEndJson.isBefore(dateStartJson)) {
				throw new Exception("La hora de llegada no puede ser anterior a la hora de salida");
			}
			// Lanza error si la fecha de salida es igual a la fecha de finalización
			if (dateEndJson.isEqual(dateStartJson)) {
				throw new Exception("La hora de salida no puede ser igual a la hora de llegada");
			}

			// Lanza error si la fecha de salida es cercana a la fecha de finalización
			if (dateStartJson.plusMinutes(5).isAfter(dateEndJson)) {
				throw new Exception("La hora de salida no puede ser tan cercana a la hora de llegada");
			}

			var dateNowZone = ZonedDateTime.now();
			dateNowZone = dateNowZone.withZoneSameInstant(ZoneId.of(TIME_ZONE));

			// Lanza error si la fecha de salida no dista una hora de la fecha actual
			if (dateStartJson.isBefore(dateNowZone.toLocalDateTime().plusMinutes(50))) {
				throw new Exception("El viaje debe ser publicado, al menos, con una hora de antelación");
			}

			var trip1 = new Trip(startingPoint, endingPoint, price, dateStartJson, dateEndJson, comments, placesJson,
					currentUser);

			trip1.setCancelationDateLimit(cancelationDateLimit);

			tripRepository.save(trip1);
			return trip1;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PreAuthorize("hasRole('ROLE_DRIVER')")
	@PostMapping("/cancel_trip_driver/{trip_id}")
	public Trip cancelTripDriver(@PathVariable(value = "trip_id") String tripId) {
		try {
			var trip1 = tripRepository.findById(new ObjectId(tripId));

			Boolean canceled = trip1.getCanceled();

			// Compruebo si el conductor del viaje es el usuario logueado
			User driver = trip1.getDriver();

			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var currentUser = userRepository.findByEmail(authentication.getPrincipal().toString());

			if (!((currentUser.getId()).equals(driver.getId()))) {
				throw new Exception("Usted no ha realizado este viaje");
			}

			// Si ya esta cencelado, envía un mensaje y no modifica el viaje
			if (canceled == true) {
				throw new Exception("El viaje ya está cancelado");
			}

			var dateStartZone = ZonedDateTime.now();
			dateStartZone = dateStartZone.withZoneSameInstant(ZoneId.of(TIME_ZONE));
			var now = dateStartZone.toLocalDateTime();

			trip1.setCanceled(true);
			trip1.setCancelationDate(now);

			refundController.createRefundDriverCancelTrip(trip1);

			tripRepository.save(trip1);
			return trip1;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}

	}

	@PreAuthorize("hasRole('ROLE_DRIVER')")
	@GetMapping("/list_trips_driver")
	public List<Trip> listTripsDriver() {
		try {
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var currentUser = userRepository.findByEmail(authentication.getPrincipal().toString());
			return tripRepository.findByDriver(currentUser);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping("/trip/{tripId}")
	public @ResponseBody Trip getTripDetails(@PathVariable(value = "tripId") String tripId) {
		try {
			var trip = tripRepository.findById(new ObjectId(tripId));
			List<TripOrder> tripOrders = tripOrderRepository.findByTripAndStatus(trip, "PAID");
			trip.setTripOrders(tripOrders);
			return trip;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@PreAuthorize("hasRole('ROLE_DRIVER')")
	@PostMapping("/trip-order/{trip_order_id}/cancel")
	public Trip cancelUserFromTrip(@PathVariable(value = "trip_order_id") String tripOrderId) {
		try {
			var tripOrder = tripOrderRepository.findById(new ObjectId(tripOrderId));
			var trip = tripOrder.getTrip();
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var currentDriver = userRepository.findByEmail(authentication.getPrincipal().toString());
			if (currentDriver.getId().equals(trip.getDriver().getId())) {
				Integer orderPlaces = tripOrder.getPlaces();
				trip.setPlaces(trip.getPlaces() + orderPlaces);
				tripRepository.save(trip);
				refundController.createRefundDriverRejection(tripOrder);
			}
			return trip;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}
}
