import { mount } from 'svelte';
import App from './App.svelte';
import './style.css';
import '@fontsource-variable/inter';
import '@fontsource/ibm-plex-mono/400.css';
mount(App, { target: document.getElementById('app')! });
