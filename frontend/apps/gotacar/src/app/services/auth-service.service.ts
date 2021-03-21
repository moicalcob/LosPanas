import { Injectable, NgZone } from '@angular/core';
import { AngularFireAuth } from '@angular/fire/auth';
import {
  AngularFirestore,
  AngularFirestoreDocument,
} from '@angular/fire/firestore';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { User } from '../shared/services/user';
import auth from 'firebase/app';

@Injectable({
  providedIn: 'root',
})
export class AuthServiceService {
  userData: any;

  constructor(
    public afs: AngularFirestore,
    public afAuth: AngularFireAuth,
    public router: Router,
    public ngZone: NgZone,
    private _http_client: HttpClient
  ) {
    this.afAuth.authState.subscribe((user) => {
      if (user) {
        this.userData = user;
        localStorage.setItem('user', JSON.stringify(this.userData));
        JSON.parse(localStorage.getItem('user'));
      } else {
        localStorage.setItem('user', null);
        JSON.parse(localStorage.getItem('user'));
      }
    });
  }

  sign_in(email, password) {
    return this.afAuth
      .signInWithEmailAndPassword(email, password)
      .then((result) => {
        this.ngZone.run(() => {
          this.router.navigate(['home']);
        });
        this.set_user_data(result.user);
      })
      .catch((error) => {
        window.alert(error.message);
      });
  }

  sign_up(email, password) {
    return this.afAuth
      .createUserWithEmailAndPassword(email, password)
      .then((result) => {
        this.send_verification_mail();
        this.set_user_data(result.user);
      })
      .catch((error) => {
        window.alert(error.message);
      });
  }

  async send_verification_mail() {
    return this.afAuth.currentUser.then((u) =>
      u.sendEmailVerification().then(() => {
        this.router.navigate(['verify-email-address']);
      })
    );
  }

  forgot_password(passwordResetEmail) {
    return this.afAuth
      .sendPasswordResetEmail(passwordResetEmail)
      .then(() => {
        window.alert('Password reset email sent, check your inbox.');
      })
      .catch((error) => {
        window.alert(error);
      });
  }

  is_logged_in(): boolean {
    const user = JSON.parse(localStorage.getItem('user'));
    return user !== null && user.emailVerified !== false ? true : false;
  }

  google_auth() {
    return this.auth_login(new auth.auth.GoogleAuthProvider());
  }

  auth_login(provider) {
    return this.afAuth
      .signInWithPopup(provider)
      .then((result) => {
        this.ngZone.run(() => {
          this.router.navigate(['authenticated', 'profile']);
        });
        this.set_user_data(result.user);
      })
      .catch((error) => {
        window.alert(error);
      });
  }

  async set_user_data(user) {
    const userRef: AngularFirestoreDocument<any> = this.afs.doc(
      `users/${user.uid}`
    );
    const userData: User = {
      id: user.id,
      firstName: user.firstName,
      lastName: user.lastName,
      uid: user.uid,
      email: user.email,
      dni: user.dni,
      profilePhoto: user.profilePhoto,
      birthdate: user.birthdate,
      roles: user.roles,
      token: user.token,
      emailVerified: user.emailVerified,
    };

    const token = await this.get_token(userData.uid)
    console.log(token)
    return userRef.set(userData, {
      merge: true,
    });
  }

  get_token(user_uid) {
    return this._http_client
    .post(environment.api_url + '/user', null, {
      params: {
        uid: user_uid
      }
    })
    .toPromise();
    
  }

  sign_out() {
    return this.afAuth.signOut().then(() => {
      localStorage.removeItem('user');
      this.router.navigate(['log-in']);
    });
  }
}
