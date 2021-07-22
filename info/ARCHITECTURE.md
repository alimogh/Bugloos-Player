# Architecture

This document is designed to provide a simple overview of Auxio's architecture and where code resides/should reside. It will be updated as aspects about Auxio change.

#### Code structure

Auxio's codebase is mostly centered around 4 different types of code.

- UIs: Fragments, RecyclerView items, and Activities are part of this class. All of them should have little data logic in them and should primarily focus on displaying information in their UIs.
- ViewModels: These usually contain data and values that a UI can display, along with doing data processing. The data often takes the form of `MutableLiveData` or `LiveData`, which can be observed.
- Shared Objects: These are the fundamental building blocks of Auxio, and exist at the process level. These are usually retrieved using `getInstance` or a similar function. Shared Objects should be avoided in UIs, as their volatility can cause problems. Its better to use a ViewModel and their exposed data instead.
- Utilities: These are largely found in the `.ui`, `.music`, and `.coil` packages, taking the form of standalone or extension functions that can be used anywhere.

Ideally, UIs should only be talking to ViewModels, ViewModels should only be talking to the Shared Objects, and Shared Objects should only be talking to other shared objects. All objects can use the utility functions.

#### UI Structure

Auxio only has one activity, that being `MainActivity`. When adding a new UI, it should be added as a `Fragment` or a `RecyclerView` item depending on the situation. 

Databinding should *always* be used instead of `findViewById`. Use `by memberBinding` if the binding needs to be a member variable in order to avoid memory leaks.

Usually, fragment creation is done in `onCreateView`, and organized into three parts:

- Create variables [Bindings, Adapters, etc]
- Set up the UI
- Set up LiveData observers

When creating a ViewHolder for a `RecyclerView`, one should use `BaseViewHolder` to standardize the binding process and automate some code shared across all ViewHolders.

Data is often bound using Binding Adapters, which are XML attributes assigned in layout files that can automatically display data, usually written as `app:bindingAdapterName="@{data}"`. Its recommended to use these instead of duplicating code manually.

#### Integers

Integer representations of data/ui elements are used heavily in Auxio. 
To prevent any strange bugs, all integer representations must be unique. A table of all current integers used are shown below:

```
0xA0XX | UI Integer Space [Required by android]

0xA000 | SongViewHolder
0xA001 | AlbumViewHolder
0xA002 | ArtistViewHolder
0xA003 | GenreViewHolder
0xA004 | HeaderViewHolder

0xA005 | QueueSongViewHolder
0xA006 | UserQueueHeaderViewHolder

0xA007 | AlbumHeaderViewHolder
0xA008 | AlbumSongViewHolder
0xA009 | ArtistHeaderViewHolder
0xA00A | ArtistAlbumViewHolder
0xA00B | ArtistSongHeaderViewHolder
0xA00C | ArtistSongViewHolder
0xA00D | GenreHeaderViewHolder
0xA00E | GenreSongViewHolder

0xA0A0 | Auxio notification code
0xA0C0 | Auxio request code

0xA1XX | Data Integer Space [Stored for IO efficency]

0xA100 | LoopMode.NONE
0xA101 | LoopMode.ALL
0xA102 | LoopMode.TRACK

0xA103 | PlaybackMode.IN_GENRE
0xA104 | PlaybackMode.IN_ARTIST
0xA105 | PlaybackMode.IN_ALBUM
0xA106 | PlaybackMode.ALL_SONGS

0xA107 | DisplayMode.SHOW_ALL
0xA108 | DisplayMode.SHOW_GENRES
0xA109 | DisplayMode.SHOW_ARTISTS
0xA10A | DisplayMode.SHOW_ALBUMS
0xA10B | DisplayMode.SHOW_SONGS

0xA10C | SortMode.NONE 
0xA10D | SortMode.ALPHA_UP
0xA10E | SortMode.ALPHA_DOWN 
0xA10F | SortMode.NUMERIC_UP
0xA110 | SortMode.NUMERIC_DOWN
```

#### Package structure overview

Auxio's package structure is mostly based around the features, and then any sub-features or components involved with that. There are some shared packages however. A diagram of the package structure is shown below:

```
hfathi.auxio  # Main UI's and logging utilities
├──.coil           # Fetchers and utilities for Coil, contains binding adapters than be used in the user interface.
├──.database       # Databases and their items for Auxio
├──.detail         # UIs for more album/artist/genre details
│  └──.adapters    # RecyclerView adapters for the detail UIs, which display the header information and items
├──.library        # Library UI
├──.loading        # Loading UI
├──.music          # Music storage and loading
├──.playback       # Playback UI and systems
│  ├──.queue       # Queue user interface
│  ├──.state       # Backend/Modes for the playback state
│  └──.system      # System-side playback [Services, ExoPlayer]
├──.recycler       # Shared RecyclerView utilities and modes
│  └──.viewholders # Shared ViewHolders and ViewHolder utilities
├──.search         # Search UI
├──.settings       # Settings UI and systems
│  ├──.blacklist   # Excluded Directories UI/Systems
│  ├──.accent      # Accent UI + Systems
│  └──.ui          # Settings-Related UIs
├──.songs          # Songs UI
└──.ui             # Shared user interface utilities
```

#### `.coil`

[Coil](https://github.com/coil-kt/coil) is the image loader used by Auxio. All image loading is done through these four functions/binding adapters:

- `app:albumArt`: Binding Adapter that will load the cover art for a song or album
- `app:artistImage`: Binding Adapter that will load the artist image
- `app:genreImage`: Binding Adapter that will load the genre image
- `loadBitmap`: Function that will take a song and return a bitmap, this should not be used in anything UI related, that is what the binding adapters above are for.

This should be enough to cover most use cases in Auxio. There are also fetchers for artist/genre images and album covers, but these are not used outside of the module.

#### `.database`

This is the general repository for databases in Auxio, along with their entities. All databases use `SQLiteOpenHelper`, with all database entities having their keys in a `companion object`.

#### `.detail`

Contains all the detail UIs for some data types in Auxio. All detail user interfaces share the same base layout (A Single RecyclerView) and only change the adapter/data being used. The adapters display both the header with information and the child items of the item itself, usually with a data list similar to this:

`Item being displayed | Child Item | Child Item | Child Item...`

`DetailViewModel` acts as the holder for the currently displaying items, along with having the `navToItem` LiveData that coordinates menu/playback navigation [Such as when a user presses "Go to artist"]

#### `.library`

The UI and adapters for the library view in Auxio, `LibraryViewModel` handles the sorting and which data to display in the fragment, while `LibraryFragment` and `LibraryAdapter` displays the data.

#### `.music`

The music loading system is based off of `MediaStore`, and loads the entire library into a variety of data objects.

All music objects inherit `BaseModel`, which guarantees that all music has both an ID and a name.

- Songs are the most basic element, with them having a reference to their album and genre. 
- Albums contain a list of their songs and their parent artist.
- Artists contain a list of songs, a list of albums, and their most prominent genre.
- Genres contain a list of songs, its preferred to use `displayName` with genres as that will convert the any numbered names into non-numbered names.

`BaseModel` can be used as an argument type to specify that any music type, while `Parent` can be used as an argument type to only specify music objects that have child items, such as albums or artists.

#### `.playback`

Auxio's playback system is somewhat unorthodox, as it avoids a lot of the built-in android code in favor of a more understandable and controllable system. Its structured around a couple of objects, the connections being highlighted in this diagram.

```
    Playback UIs    Queue UI    PlaybackService
         │             │               │
         │             │               │
 PlaybackViewModel─────┘               │
         │                             │
         │                             │
PlaybackStateManager───────────────────┘
```

`PlaybackStateManager` is the shared object that contains the master copy of the playback state, doing all operations on it. This object should ***NEVER*** be used in a UI, as it does not sanitize input and can cause major problems if a Volatile UI interacts with it. It's callback system is also prone to memory leaks if not cleared when done. `PlaybackViewModel` should be used instead, as it exposes stable data and safe functions that UIs can use to interact with the playback state.

`PlaybackService`'s job is to use the playback state to manage the ExoPlayer instance and notification and also modify the state depending on system events, such as when a button is pressed on a headset. It should **never** be bound to, mostly because there is no need given that `PlaybackViewModel` exposes the same data in a much safer fashion. `PlaybackService` also controls the `PlaybackSessionConnector` and `AudioReactor` classes, which manage the `MediaSession` and `AudioFocus` state respectively.

#### `.recycler`

Shared RecyclerView utilities, often for adapters and ViewHolders. Important ones to note are `DiffCallback`, which acts as a reusable differ callback based off of `BaseModel` for `ListAdapter`s, and the shared ViewHolders for each data type, such as `SongViewHolder` or `HeaderViewHolder`.

#### `.settings`

The settings system is primarily based off of `SettingsManager`, a wrapper around `SharedPreferences`. This allows settings to be read/written in a much simpler/safer manner and without a context being needed. The Settings UI is largely contained in `SettingsListFragment`, while the sub-packages contain sub-uis related to the `SettingsListFragment`, such as the custom list preference `IntListPreference` and the about dialog.

#### `.search`

Package for Auxio's search functionality, `SearchViewHolder` handles the data results and filtering while `SearchFragment`/`SearchAdapter` handles the display of the results and user input.

#### `.songs`

Package for the songs UI, there is no data management here, only a user interface.

#### `.ui`

Shared User Interface utilities. This is primarily made up of convenience/extension functions relating to Views, Resources, Configurations, and Contexts. It also contains some dedicated utilities, such as:
- The Accent Management system
- `newMenu` and `ActionMenu`, which automates menu creation for most data types
- `memberBinding` and `MemberBinder`, which allows for ViewBindings to be used as a member variable without memory leaks or nullability issues.