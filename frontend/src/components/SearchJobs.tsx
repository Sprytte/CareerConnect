import { useMemo, useState } from "react";
import "../styles/SearchJobs.css";
import SiteHeader from "./SiteHeader";
import SiteFooter from "./SiteFooter";
import { useSession } from "../auth/session";

type JobPosting = {
  id: number;
  title: string;
  company: string;
  location: string;
  category: string;
  description: string;
  createdAt: string;
  deadline: string;
};

const mockJobs: JobPosting[] = [
  {
    id: 1,
    title: "Software Developer Intern",
    company: "CareerConnect",
    location: "Montreal, QC",
    category: "Software Engineering",
    description:
      "Work with a development team to build and improve web applications, test new features, and contribute to software design. You will collaborate with frontend and backend developers to implement new functionality, troubleshoot bugs, improve application performance, and maintain clean and reusable code. You will also participate in code reviews, team meetings, and feature planning sessions. The role may involve working with React, TypeScript, Java, REST APIs, databases, and Git-based development workflows. You will help test features before release, document technical changes, communicate progress with other team members, and contribute ideas for improving the overall user experience. This position is intended for students or junior developers who want practical experience working on a real software product in a collaborative development environment.",
    createdAt: "2026-10-08",
    deadline: "2026-11-15",
  },
  {
    id: 2,
    title: "Junior Embedded Systems Developer",
    company: "Tech Systems",
    location: "Montreal, QC",
    category: "Computer Engineering",
    description:
      "Assist with embedded software development, testing, debugging, and integration of connected systems. You will work with microcontrollers, sensors, communication protocols, and hardware interfaces while collaborating with software and electrical engineering teams.",
    createdAt: "2026-10-06",
    deadline: "2026-11-20",
  },
  {
    id: 3,
    title: "Frontend Developer",
    company: "Digital Labs",
    location: "Toronto, ON",
    category: "Software Engineering",
    description:
      "Build responsive user interfaces and collaborate with designers and backend developers to deliver polished web experiences. You will create reusable React components, improve accessibility, and test application behavior across devices.",
    createdAt: "2026-10-04",
    deadline: "2026-11-10",
  },
  {
    id: 4,
    title: "Data Analyst Intern",
    company: "Insight Analytics",
    location: "Ottawa, ON",
    category: "Data",
    description:
      "Analyze datasets, prepare reports, and help identify trends that support business decisions. You will clean and organize data, create visual summaries, and communicate findings to team members.",
    createdAt: "2026-10-02",
    deadline: "2026-11-25",
  },
];

export default function SearchJobs() {
  const session = useSession();

  const [keyword, setKeyword] = useState("");
  const [location, setLocation] = useState("");
  const [category, setCategory] = useState("");
  const [expandedJobId, setExpandedJobId] = useState<number | null>(null);

  const categories = Array.from(
    new Set(mockJobs.map((job) => job.category))
  );

  const filteredJobs = useMemo(() => {
    const keywordValue = keyword.trim().toLowerCase();
    const locationValue = location.trim().toLowerCase();

    return mockJobs.filter((job) => {
      const matchesKeyword =
        keywordValue === "" ||
        job.title.toLowerCase().includes(keywordValue) ||
        job.company.toLowerCase().includes(keywordValue) ||
        job.description.toLowerCase().includes(keywordValue);

      const matchesLocation =
        locationValue === "" ||
        job.location.toLowerCase().includes(locationValue);

      const matchesCategory =
        category === "" || job.category === category;

      return matchesKeyword && matchesLocation && matchesCategory;
    });
  }, [keyword, location, category]);

  const clearFilters = () => {
    setKeyword("");
    setLocation("");
    setCategory("");
  };

  const formatDate = (date: string) => {
    const [year, month, day] = date.split("-");

    return new Date(
      Number(year),
      Number(month) - 1,
      Number(day)
    ).toLocaleDateString();
  };

  return (
    <div className="search-jobs-page">
      <SiteHeader />

      <main className="search-jobs-main">
        <section className="search-jobs-heading">
          <span className="search-jobs-kicker">
            FIND YOUR NEXT MOVE
          </span>

          <h1>Search for Jobs</h1>

          <p>
            Explore available opportunities and narrow your search by
            keyword, location, or category.
          </p>

          {session.status === "loading" && (
            <p className="search-jobs-message">
              Checking account...
            </p>
          )}

          {session.status === "unavailable" && (
            <p className="search-jobs-message">
              Account information is currently unavailable, but you can
              still browse jobs.
            </p>
          )}
        </section>

        <section className="job-search-filters">
          <div className="job-search-field">
            <label htmlFor="job-keyword">Keyword</label>

            <input
              id="job-keyword"
              type="text"
              placeholder="Job title, company, or keyword"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-location">Location</label>

            <input
              id="job-location"
              type="text"
              placeholder="Montreal, Toronto..."
              value={location}
              onChange={(event) => setLocation(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-category">Category</label>

            <select
              id="job-category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              <option value="">All categories</option>

              {categories.map((jobCategory) => (
                <option key={jobCategory} value={jobCategory}>
                  {jobCategory}
                </option>
              ))}
            </select>
          </div>

          <button
            className="clear-filters-button"
            type="button"
            onClick={clearFilters}
          >
            Clear filters
          </button>
        </section>

        <div className="search-results-heading">
          <h2>Available Jobs</h2>

          <span>
            {filteredJobs.length}{" "}
            {filteredJobs.length === 1 ? "result" : "results"}
          </span>
        </div>

        {filteredJobs.length === 0 ? (
          <section className="search-jobs-empty">
            <h2>No jobs found</h2>

            <p>
              Try changing your keyword, location, or category.
            </p>
          </section>
        ) : (
          <section className="search-jobs-grid">
            {filteredJobs.map((job) => {
              const isExpanded = expandedJobId === job.id;

              return (
                <article className="search-job-card" key={job.id}>
                  <span className="search-job-category">
                    {job.category}
                  </span>

                  <h2>{job.title}</h2>

                  <div className="search-job-details">
                    <span>{job.company}</span>
                    <span>•</span>
                    <span>{job.location}</span>
                  </div>

                  <p
                    className={
                      isExpanded
                        ? "search-job-description expanded"
                        : "search-job-description"
                    }
                  >
                    {job.description}
                  </p>

                  <div className="search-job-footer">
                    <div className="search-job-dates">
                      <span>
                        Posted {formatDate(job.createdAt)}
                      </span>

                      <span>
                        Deadline {formatDate(job.deadline)}
                      </span>
                    </div>

                    <button
                      className="view-details-button"
                      type="button"
                      onClick={() =>
                        setExpandedJobId(
                          isExpanded ? null : job.id
                        )
                      }
                    >
                      {isExpanded ? "Hide details" : "View details"}
                    </button>
                  </div>
                </article>
              );
            })}
          </section>
        )}
      </main>

      <SiteFooter />
    </div>
  );
}