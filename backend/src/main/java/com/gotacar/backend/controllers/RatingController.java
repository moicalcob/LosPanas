package com.gotacar.backend.controllers;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gotacar.backend.models.User;
import com.gotacar.backend.models.UserRepository;
import com.gotacar.backend.models.rating.Rating;
import com.gotacar.backend.models.rating.RatingRepository;
import com.gotacar.backend.models.trip.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST })
public class RatingController {

	private static ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TripRepository tripRepository;

	@Autowired
	private RatingRepository ratingRepository;

	/*
	 * @return rating creada tras valoracion Actualizacion parámetro averageRatings
	 * del usuario
	 * 
	 * @param Integer points: puntos de la valoracion String idUserTo: id del
	 * usuario al que valora String content: Mensaje de la valoracion
	 */
	@PreAuthorize("hasRole('ROLE_CLIENT')")
	@PostMapping(path = "/rate", consumes = "application/json")
	public Rating rateUser(@RequestBody() String body) {
		try {
			var jsonNode = objectMapper.readTree(body);
			String idUser = objectMapper.readTree(jsonNode.get("to").toString()).asText();
			String content = objectMapper.readTree(jsonNode.get("content").toString()).asText();
			String tripId = objectMapper.readTree(jsonNode.get("trip_id").toString()).asText();
			Integer points = objectMapper.readTree(jsonNode.get("points").toString()).asInt();

			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var from = userRepository.findByEmail(authentication.getPrincipal().toString());
			var to = userRepository.findById(new ObjectId(idUser));
			var trip = tripRepository.findById(new ObjectId(tripId));
			if (points < 1 || points > 5) {
				throw new IllegalArgumentException("Las valoraciones no están en el rango 1-5");
			}
			if (content.equals("") ) {
				throw new IllegalArgumentException("El contenido está vacío");
			}
			if (userRepository.findById(new ObjectId(idUser)) == null) {
				throw new IllegalArgumentException(
						"El usuario al que intenta valorar no existe en nuestra base de datos");
			}
			if (ratingRepository
					.findAll().stream().filter(x -> x.getTrip().getId().equals(tripId)
							&& x.getFrom().getId().equals(from.getId()) && x.getTo().getId().equals(idUser))
					.count() != 0) {
				throw new IllegalArgumentException("El usuario al que intenta valorar ya lo ha valorado previamente");
			}

			var rate = new Rating(from, to, content, points, trip);
			ratingRepository.save(rate);

			actualizaUsuario(to);

			return rate;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	private void actualizaUsuario(User to) {
		List<Rating> lsRating = ratingRepository.findByTo(to);
		Integer total = 0;
		for (Rating r : lsRating) {
			total += r.getPoints();
		}
		Integer avg = total / lsRating.size();
		to.setAverageRatings(avg);
		userRepository.save(to);
	}

	/*
	 * @return usuarios a los que ya ha valorado el usuario actual
	 * 
	 * @param lista de usuarios de un viaje
	 */
	@PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
	@PostMapping(path = "/rate/check", consumes = "application/json")
	public List<String> rateCheck(@RequestBody() String body) {
		try {
			List<String> res = new ArrayList<>();

			var authentication = SecurityContextHolder.getContext().getAuthentication();
			var from = userRepository.findByEmail(authentication.getPrincipal().toString());

			var jsonNode = objectMapper.readTree(body);
			String idUsers = objectMapper.readTree(jsonNode.get("id_users").toString()).asText();
			String idTrip = objectMapper.readTree(jsonNode.get("trip_id").toString()).asText();

			List<String> idsUsersRatedByFrom = ratingRepository.findByFrom(from).stream()
					.filter(a -> a.getTrip().getId().equals(idTrip)).map(x -> x.getTo().getId())
					.collect(Collectors.toList());
			// Si no hay ningún usuario que haya adquirido el viaje, se devuelve la lista
			// vacía
			if (idUsers.length() <= 0) {
				return res;
			} else {
				String[] users = idUsers.split(",");
				for (var i = 0; i < users.length; i++) {
					if (idsUsersRatedByFrom.contains(users[i].trim()))
						res.add(users[i]);
				}
				return res;
			}
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}
	@PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN') || hasRole('ROLE_DRIVER')")
	@GetMapping("/ratings/{idUser}")
	public List<Rating> getRatings(@PathVariable(value = "idUser") String userId) {
		try {
			var user = userRepository.findById(new ObjectId(userId));
			return ratingRepository.findByTo(user);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

}
